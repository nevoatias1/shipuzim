package com.site.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.site.server.config.AdminBootstrap;
import com.site.server.contact.ContactRequest;
import com.site.server.contact.ContactRequestRepository;
import com.site.server.contact.RequestStatus;
import com.site.server.contact.WorkType;
import com.site.server.model.CustomUser;
import com.site.server.model.Role;
import com.site.server.service.UserService;
import com.site.server.utils.JwtUtil;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        // Synthetic test-only bootstrap credentials.
        "ADMIN_USERNAME=stage3-test-admin", "ADMIN_PASSWORD=test-only-admin-password",
        "ADMIN_FIRST_NAME=Test", "ADMIN_LAST_NAME=Admin", "ADMIN_EMAIL=stage3-admin@example.test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ContactRequestRepository requests;
    @Autowired private UserService users;
    @Autowired private JwtUtil jwt;
    @Autowired private CustomUserDetailsService userDetails;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwords;
    @Autowired private AdminBootstrap bootstrap;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        requests.deleteAll();
        if (users.getUserByUsername("stage3-test-user") == null) {
            users.register(new CustomUser("Test", "User", "stage3-user@example.test", null, null,
                    "stage3-test-user", "test-only-user-password", Role.USER));
        }
    }

    @Test
    void adminListRejectsAnonymousAndUserButAcceptsAdmin() throws Exception {
        mockMvc.perform(get("/admin/contact-requests")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/contact-requests").header("Authorization", token("stage3-test-user")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/contact-requests").header("Authorization", token("stage3-test-admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void listIsNewestFirstWithDefaultTwentyAndSupportsPagination() throws Exception {
        var saved = requests.saveAll(IntStream.range(0, 21)
                .mapToObj(index -> new ContactRequest("Test " + index, "0501234567", null,
                        null, WorkType.CLADDING, null)).toList());
        mockMvc.perform(get("/admin/contact-requests").header("Authorization", token("stage3-test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(20)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.items[0].id").value(saved.getLast().getId()))
                .andExpect(jsonPath("$.items[0].fullName").value(saved.getLast().getFullName()))
                .andExpect(jsonPath("$.items[0].phone").value("0501234567"))
                .andExpect(jsonPath("$.items[0].workType").value("CLADDING"))
                .andExpect(jsonPath("$.items[0].status").value("NEW"))
                .andExpect(jsonPath("$.items[0].createdAt").isNotEmpty());
        mockMvc.perform(get("/admin/contact-requests?page=1&size=20")
                        .header("Authorization", token("stage3-test-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items[0].id").value(saved.getFirst().getId()));
    }

    @Test
    void adminCanChangeStatusAndUnknownIdReturns404() throws Exception {
        ContactRequest saved = requests.save(new ContactRequest("Test Name", "0501234567", null,
                null, WorkType.PAINTING, null));
        mockMvc.perform(put("/admin/contact-requests/{id}/status", saved.getId())
                        .header("Authorization", token("stage3-test-user"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HANDLED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/admin/contact-requests/{id}/status", saved.getId())
                        .header("Authorization", token("stage3-test-admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HANDLED\"}"))
                .andExpect(status().isNoContent());
        assertThat(requests.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(RequestStatus.HANDLED);
        mockMvc.perform(put("/admin/contact-requests/{id}/status", Long.MAX_VALUE)
                        .header("Authorization", token("stage3-test-admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"NEW\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void correctAdminCredentialsReturnJwtAndWrongPasswordReturns403() throws Exception {
        String response = mockMvc.perform(authenticate("203.0.113.1", "test-only-admin-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("jwt").asText();
        assertThat(jwt.extractUsername(token)).isEqualTo("stage3-test-admin");
        mockMvc.perform(get("/admin/contact-requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(authenticate("203.0.113.2", "wrong-test-password"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationRequiresAToken() throws Exception {
        mockMvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void startupBootstrapUsesBcryptAndDoesNotOverwriteAnExistingAdmin() {
        CustomUser admin = users.getUserByUsername("stage3-test-admin");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getFirstName()).isEqualTo("Test");
        assertThat(admin.getLastName()).isEqualTo("Admin");
        assertThat(admin.getEmail()).isEqualTo("stage3-admin@example.test");
        assertThat(admin.getPassword()).startsWith("$2");
        assertThat(passwords.matches("test-only-admin-password", admin.getPassword())).isTrue();
        bootstrap.run(new DefaultApplicationArguments());
        bootstrap.run(new DefaultApplicationArguments());
        assertThat(users.getUserByUsername(admin.getUsername()).getPassword()).isEqualTo(admin.getPassword());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class,
                admin.getUsername())).isEqualTo(1);
    }

    @Test
    void authenticationHasItsOwnIpRateLimit() throws Exception {
        mockMvc.perform(authenticate("203.0.113.3", "wrong-test-password")).andExpect(status().isForbidden());
        mockMvc.perform(authenticate("203.0.113.3", "wrong-test-password")).andExpect(status().isForbidden());
        mockMvc.perform(authenticate("203.0.113.3", "wrong-test-password"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
        mockMvc.perform(post("/api/contact").with(request -> {
            request.setRemoteAddr("203.0.113.3");
            return request;
        }).contentType(MediaType.APPLICATION_JSON).content("{\"website\":\"bot\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void configuredCorsOriginCanPreflightAdminPutWithAuthorization() throws Exception {
        mockMvc.perform(options("/admin/contact-requests/1/status")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mockMvc.perform(options("/authenticate")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    private String token(String username) {
        return "Bearer " + jwt.generateToken(userDetails.loadUserByUsername(username));
    }

    private MockHttpServletRequestBuilder authenticate(String ip, String password) throws Exception {
        return post("/authenticate").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        }).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new com.site.server.model.AuthenticationRequest(
                        "stage3-test-admin", password)));
    }
}
