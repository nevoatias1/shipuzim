package com.site.server.serviceimage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.site.server.model.CustomUser;
import com.site.server.model.Role;
import com.site.server.security.CustomUserDetailsService;
import com.site.server.service.UserService;
import com.site.server.utils.JwtUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceImageIntegrationTest {
    @TempDir static Path uploads;
    @Autowired MockMvc mvc;
    @Autowired ServiceImageRepository repository;
    @Autowired UserService users;
    @Autowired JwtUtil jwt;
    @Autowired CustomUserDetailsService details;
    @Autowired ObjectMapper mapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.gallery.local-directory", () -> uploads.toString());
    }

    @BeforeEach
    void prepare() throws Exception {
        cleanup();
        for (Role role : List.of(Role.ADMIN, Role.USER)) {
            String name = "service-image-" + role.name();
            if (users.getUserByUsername(name) == null) users.register(new CustomUser(
                    "Service", "QA", name + "@example.test", null, null, name, "synthetic-test-password", role));
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        repository.deleteAll();
        try (var files = Files.list(uploads)) {
            for (var file : files.toList()) Files.delete(file);
        }
    }

    @ParameterizedTest
    @EnumSource(ServiceKey.class)
    void adminCanUploadReplaceAndDeleteEachServiceImage(ServiceKey key) throws Exception {
        mvc.perform(get("/api/service-images")).andExpect(status().isOk()).andExpect(content().json("{}"));
        String response = mvc.perform(multipart(HttpMethod.PUT, "/admin/service-images/{key}", key)
                        .file(png()).header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imageUrl").isString())
                .andReturn().getResponse().getContentAsString();
        ServiceImage first = repository.findById(key).orElseThrow();
        String firstKey = first.getStorageKey();
        var updatedAt = first.getUpdatedAt();
        assertThat(Files.exists(uploads.resolve(firstKey))).isTrue();
        String url = mapper.readTree(response).get("imageUrl").asText();
        mvc.perform(get("/api/service-images")).andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(java.util.Map.of(key.name(), url))));
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(multipart(HttpMethod.PUT, "/admin/service-images/{key}", key)
                        .file(png()).header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isOk());
        ServiceImage replaced = repository.findById(key).orElseThrow();
        assertThat(replaced.getStorageKey()).isNotEqualTo(firstKey);
        assertThat(replaced.getUpdatedAt()).isAfterOrEqualTo(updatedAt);
        assertThat(uploads.resolve(firstKey)).doesNotExist();
        assertThat(uploads.resolve(replaced.getStorageKey())).exists();
        assertThat(repository.count()).isEqualTo(1);
        mvc.perform(delete("/admin/service-images/{key}", key).header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isNoContent());
        assertThat(repository.findById(key)).isEmpty();
        assertThat(uploads.resolve(replaced.getStorageKey())).doesNotExist();
        mvc.perform(get("/api/service-images")).andExpect(status().isOk()).andExpect(content().json("{}"));
        mvc.perform(delete("/admin/service-images/{key}", key).header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void mutationsRejectAnonymousAndUserTokens() throws Exception {
        for (String authorization : List.of("", token(Role.USER))) {
            mvc.perform(multipart(HttpMethod.PUT, "/admin/service-images/PAINTING").file(png())
                            .header("Authorization", authorization)).andExpect(status().isForbidden());
            mvc.perform(delete("/admin/service-images/PAINTING").header("Authorization", authorization))
                    .andExpect(status().isForbidden());
        }
        assertThat(repository.count()).isZero();
        try (var files = Files.list(uploads)) { assertThat(files.count()).isZero(); }
    }

    @Test
    void rejectsUnknownKeysAndInvalidOrMissingImagesWithoutChangingExistingImage() throws Exception {
        mvc.perform(multipart(HttpMethod.PUT, "/admin/service-images/PAINTING").file(png())
                        .header("Authorization", token(Role.ADMIN))).andExpect(status().isOk());
        String oldKey = repository.findById(ServiceKey.PAINTING).orElseThrow().getStorageKey();
        mvc.perform(multipart(HttpMethod.PUT, "/admin/service-images/PAINTING")
                        .file(new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1, 2, 3}))
                        .header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_IMAGE"));
        mvc.perform(multipart(HttpMethod.PUT, "/admin/service-images/PAINTING")
                        .header("Authorization", token(Role.ADMIN))).andExpect(status().isBadRequest());
        mvc.perform(multipart(HttpMethod.PUT, "/admin/service-images/UNKNOWN").file(png())
                        .header("Authorization", token(Role.ADMIN))).andExpect(status().isBadRequest());
        mvc.perform(delete("/admin/service-images/UNKNOWN").header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isBadRequest());
        assertThat(repository.findById(ServiceKey.PAINTING).orElseThrow().getStorageKey()).isEqualTo(oldKey);
        try (var files = Files.list(uploads)) { assertThat(files.count()).isEqualTo(1); }
        mvc.perform(post("/api/service-images")).andExpect(status().isForbidden());
    }

    private String token(Role role) {
        return "Bearer " + jwt.generateToken(details.loadUserByUsername("service-image-" + role.name()));
    }

    private MockMultipartFile png() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("file", "photo.png", "image/png", bytes.toByteArray());
    }
}
