package com.site.server.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.site.server.model.CustomUser;
import com.site.server.model.Role;
import com.site.server.security.CustomUserDetailsService;
import com.site.server.service.UserService;
import com.site.server.utils.JwtUtil;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.reviews.rate-limit.limit=2")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ReviewRepository reviews;
    @Autowired UserService users;
    @Autowired CustomUserDetailsService details;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    private static final AtomicInteger IPS = new AtomicInteger();

    @BeforeEach
    void prepare() {
        reviews.deleteAll();
        for (String name : new String[]{"one", "two", "admin"}) {
            String username = "stage7-review-" + name;
            if (users.getUserByUsername(username) == null) {
                users.register(new CustomUser("Test", "Person", username + "@example.test", null, null,
                        username, "synthetic-test-password", "admin".equals(name) ? Role.ADMIN : Role.USER));
            }
        }
    }

    @Test
    void createThenEditASecondSubmissionUpdatesSameOwnReviewOnly() throws Exception {
        Review other = reviews.saveAndFlush(new Review("stage7-review-two", 2, "Other person's review"));
        String first = mvc.perform(write("one", "PUT").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("rating", 5, "text", "  Good\nwork\u0000\u202e  ",
                                "username", "stage7-review-two", "id", other.getId()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.text").value("Goodwork"))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(first).get("id").asLong();
        Review initial = reviews.findById(id).orElseThrow();
        mvc.perform(write("one", "PUT").contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4,\"text\":\" Edited \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.rating").value(4)).andExpect(jsonPath("$.text").value("Edited"));
        Review edited = reviews.findById(id).orElseThrow();
        assertThat(edited.getUsername()).isEqualTo("stage7-review-one");
        assertThat(edited.getCreatedAt()).isEqualTo(initial.getCreatedAt());
        assertThat(edited.getUpdatedAt()).isAfterOrEqualTo(initial.getUpdatedAt());
        assertThat(reviews.count()).isEqualTo(2);
        assertThat(reviews.findById(other.getId()).orElseThrow().getText()).isEqualTo("Other person's review");
        mvc.perform(get("/users/review").header("Authorization", token("one")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(write("one", "DELETE").param("username", "stage7-review-two")).andExpect(status().isNoContent());
        assertThat(reviews.existsById(id)).isFalse();
        assertThat(reviews.existsById(other.getId())).isTrue();
    }

    @Test
    void anonymousCannotReadOrWriteOwnReviewAndMissingOwnReturns404() throws Exception {
        mvc.perform(get("/users/review")).andExpect(status().isForbidden());
        mvc.perform(put("/users/review").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"text\":\"Test\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/users/review")).andExpect(status().isForbidden());
        reviews.saveAndFlush(new Review("stage7-review-two", 3, "Other"));
        mvc.perform(get("/users/review").header("Authorization", token("one")))
                .andExpect(status().isNotFound());
        mvc.perform(write("one", "DELETE")).andExpect(status().isNotFound());
        assertThat(reviews.count()).isEqualTo(1);
    }

    @Test
    void publicListIsPaginatedNewestFirstWithGlobalStatisticsAndPrivateAuthorFieldsHidden() throws Exception {
        Review older = reviews.saveAndFlush(new Review("stage7-review-one", 5, "First"));
        Review newer = reviews.saveAndFlush(new Review("stage7-review-two", 3, "Second"));
        mvc.perform(get("/api/reviews?page=0&size=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(newer.getId()))
                .andExpect(jsonPath("$.items[0].displayName").value("Test P."))
                .andExpect(jsonPath("$.items[0].rating").value(3))
                .andExpect(jsonPath("$.items[0].text").value("Second"))
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.items[0].authorEmail").doesNotExist())
                .andExpect(jsonPath("$.items[0].username").doesNotExist())
                .andExpect(jsonPath("$.items[0].lastName").doesNotExist())
                .andExpect(jsonPath("$.count").value(2)).andExpect(jsonPath("$.average").value(4.0))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2));
        mvc.perform(get("/api/reviews?page=1&size=1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(older.getId()));
        mvc.perform(post("/api/reviews")).andExpect(status().isForbidden());
    }

    @Test
    void adminListIncludesEmailAndAdminDeleteRemovesReview() throws Exception {
        Review review = reviews.saveAndFlush(new Review("stage7-review-one", 4, "Test"));
        mvc.perform(get("/admin/reviews")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/reviews").header("Authorization", token("one"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/reviews").header("Authorization", token("admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(review.getId()))
                .andExpect(jsonPath("$.items[0].authorEmail").value("stage7-review-one@example.test"));
        mvc.perform(delete("/admin/reviews/{id}", review.getId())).andExpect(status().isForbidden());
        mvc.perform(delete("/admin/reviews/{id}", review.getId()).header("Authorization", token("one")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/admin/reviews/{id}", review.getId()).header("Authorization", token("admin")))
                .andExpect(status().isNoContent());
        assertThat(reviews.existsById(review.getId())).isFalse();
        mvc.perform(delete("/admin/reviews/{id}", review.getId()).header("Authorization", token("admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRatingAndLongTextReturnFieldErrors() throws Exception {
        for (String body : new String[]{"{\"text\":\"Test\"}", "{\"rating\":0}", "{\"rating\":6}"}) {
            mvc.perform(write("one", "PUT").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.rating").isNotEmpty());
        }
        mvc.perform(write("one", "PUT").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("rating", 5, "text", "x".repeat(501)))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.text").isNotEmpty());
        assertThat(reviews.count()).isZero();
    }

    @Test
    void reviewWriteLimiterCoversPutAndDeleteButNotReads() throws Exception {
        String ip = "192.0.2.250";
        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/users/review").with(request -> { request.setRemoteAddr(ip); return request; })
                            .header("Authorization", token("one")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\":5,\"text\":\"Test\"}"))
                    .andExpect(status().isOk());
        }
        mvc.perform(delete("/users/review").with(request -> { request.setRemoteAddr(ip); return request; })
                        .header("Authorization", token("one")))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
        mvc.perform(get("/users/review").with(request -> { request.setRemoteAddr(ip); return request; })
                        .header("Authorization", token("one"))).andExpect(status().isOk());
        mvc.perform(get("/api/reviews").with(request -> { request.setRemoteAddr(ip); return request; }))
                .andExpect(status().isOk());
    }

    @Test
    void flywayCascadesReviewsWhenTheirUserIsDeleted() {
        reviews.saveAndFlush(new Review("stage7-review-two", 4, "Test"));
        users.deleteUser("stage7-review-two");
        assertThat(reviews.count()).isZero();
    }

    @Test
    void emptyListHasZeroStatisticsAndPaginationIsBounded() throws Exception {
        mvc.perform(get("/api/reviews")).andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.count").value(0)).andExpect(jsonPath("$.average").value(0.0));
        for (String query : new String[]{"page=-1", "size=0", "size=101"}) {
            mvc.perform(get("/api/reviews?" + query)).andExpect(status().isBadRequest());
        }
    }

    private MockHttpServletRequestBuilder write(String user, String method) {
        return ("PUT".equals(method) ? put("/users/review") : delete("/users/review"))
                .header("Authorization", token(user)).with(request -> {
                    request.setRemoteAddr("192.0.2." + IPS.incrementAndGet());
                    return request;
                });
    }

    private String token(String user) {
        return "Bearer " + jwt.generateToken(details.loadUserByUsername("stage7-review-" + user));
    }
}
