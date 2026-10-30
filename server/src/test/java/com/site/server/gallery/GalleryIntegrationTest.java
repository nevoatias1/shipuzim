package com.site.server.gallery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.site.server.model.CustomUser;
import com.site.server.model.Role;
import com.site.server.security.CustomUserDetailsService;
import com.site.server.service.UserService;
import com.site.server.utils.JwtUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GalleryIntegrationTest {
    @TempDir static Path uploads;
    @Autowired MockMvc mvc;
    @Autowired GalleryItemRepository repository;
    @Autowired ObjectMapper mapper;
    @Autowired UserService users;
    @Autowired JwtUtil jwt;
    @Autowired CustomUserDetailsService details;
    @Autowired TestRestTemplate http;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.gallery.local-directory", () -> uploads.toString());
    }

    @BeforeEach
    void prepare() throws Exception {
        cleanup();
        for (Role role : List.of(Role.ADMIN, Role.USER)) {
            String username = "gallery-test-" + role.name().toLowerCase(java.util.Locale.ROOT);
            if (users.getUserByUsername(username) == null) {
                users.register(new CustomUser("Test", "User", username + "@example.test", null, null,
                        username, "synthetic-test-password", role));
            }
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        repository.deleteAll();
        try (var paths = Files.list(uploads)) {
            for (Path path : paths.toList()) Files.delete(path);
        }
    }

    @Test
    void uploadStripsExifGpsResizesAndServesPublicImageWithGeneratedName() throws Exception {
        byte[] jpeg = jpegWithExifGps(2400, 1200);
        assertThat(new String(jpeg, StandardCharsets.ISO_8859_1)).contains("Exif");
        JsonNode item = uploaded(jpeg, "../../private-name.jpg", "text/plain", "  Stone wall  ");
        GalleryItem saved = repository.findById(item.get("id").asLong()).orElseThrow();
        assertThat(saved.getCaption()).isEqualTo("Stone wall");
        assertThat(saved.getStorageKey()).matches("[a-f0-9-]{36}\\.png");
        byte[] clean = Files.readAllBytes(uploads.resolve(saved.getStorageKey()));
        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(clean));
        assertThat(image.getWidth()).isEqualTo(2000);
        assertThat(image.getHeight()).isEqualTo(1000);
        // Output chunks contain pixels only, with no EXIF/GPS or text metadata chunks.
        ByteBuffer png = ByteBuffer.wrap(clean).position(8);
        while (png.remaining() >= 12) {
            int length = png.getInt();
            byte[] type = new byte[4];
            png.get(type);
            assertThat(new String(type, StandardCharsets.US_ASCII)).isIn("IHDR", "IDAT", "IEND");
            png.position(png.position() + length + 4);
        }
        mvc.perform(get(saved.getImageUrl())).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG)).andExpect(content().bytes(clean))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/api/gallery")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(saved.getId()))
                .andExpect(jsonPath("$[0].caption").value("Stone wall"))
                .andExpect(jsonPath("$[0].category").value("CLADDING"))
                .andExpect(jsonPath("$[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$[0].sortOrder").doesNotExist())
                .andExpect(jsonPath("$[0].createdAt").doesNotExist());
        assertThat(item.size()).isEqualTo(4);
    }

    @Test
    void publicListOrdersBySortOrderThenNewestAndAdminCanUpdate() throws Exception {
        long first = uploaded(png(), "a.png", "image/png", "First").get("id").asLong();
        long second = uploaded(png(), "b.png", "image/png", "Second").get("id").asLong();
        long third = uploaded(png(), "c.png", "image/png", "Third").get("id").asLong();
        mvc.perform(put("/admin/gallery/{id}", first).header("Authorization", token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caption\":\"  Updated  \",\"category\":\"PAINTING\",\"sortOrder\":-1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.caption").value("Updated"))
                .andExpect(jsonPath("$.category").value("PAINTING"));
        assertThat(repository.findById(first).orElseThrow().getSortOrder()).isEqualTo(-1);
        mvc.perform(get("/api/gallery")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(first))
                .andExpect(jsonPath("$[1].id").value(third))
                .andExpect(jsonPath("$[2].id").value(second));
    }

    @Test
    void mutationsRejectAnonymousAndUserTokens() throws Exception {
        for (String authorization : List.of("", token(Role.USER))) {
            mvc.perform(multipart("/admin/gallery").file(file(png(), "photo.png", "image/png"))
                            .param("category", "CLADDING").header("Authorization", authorization))
                    .andExpect(status().isForbidden());
            mvc.perform(put("/admin/gallery/1").header("Authorization", authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"caption\":\"Test\",\"category\":\"CLADDING\",\"sortOrder\":0}"))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/admin/gallery/1").header("Authorization", authorization))
                    .andExpect(status().isForbidden());
        }
        assertThat(repository.count()).isZero();
    }

    @Test
    void fakeCorruptAndUnsupportedImagesAreRejectedWithoutSaving() throws Exception {
        for (byte[] bytes : List.of("not an image".getBytes(StandardCharsets.UTF_8),
                new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10},
                new byte[]{'R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P'},
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff},
                "GIF89a".getBytes(StandardCharsets.US_ASCII))) {
            upload(bytes, "renamed.jpg", "image/jpeg", "caption")
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_IMAGE"));
        }
        assertThat(repository.count()).isZero();
        try (var paths = Files.list(uploads)) { assertThat(paths.count()).isZero(); }
    }

    @Test
    void validPngAndWebpAreDetectedIndependentlyOfFilenameAndContentType() throws Exception {
        uploaded(png(), "picture.txt", "text/plain", null);
        byte[] webp = Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        uploaded(webp, "picture.exe", "application/octet-stream", null);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void oversizedFileIsRejectedBeforeDecodingOrSaving() throws Exception {
        upload(new byte[ImageSanitizer.MAX_BYTES + 1], "large.jpg", "image/jpeg", null)
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.error").value("IMAGE_TOO_LARGE"));
        assertThat(repository.count()).isZero();
    }

    @Test
    void realServletMultipartLimitReturnsClear413Json() {
        var parts = new LinkedMultiValueMap<String, Object>();
        parts.add("file", new ByteArrayResource(new byte[ImageSanitizer.MAX_BYTES + 1]) {
            @Override public String getFilename() { return "large.jpg"; }
        });
        parts.add("category", "CLADDING");
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", token(Role.ADMIN));
        var response = http.postForEntity("/admin/gallery", new HttpEntity<>(parts, headers), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().path("error").asText()).isEqualTo("IMAGE_TOO_LARGE");
        assertThat(repository.count()).isZero();
    }

    @Test
    void validatesCaptionCategoryAndRequiredImage() throws Exception {
        upload(png(), "a.png", "image/png", "a".repeat(201)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.caption").isNotEmpty());
        mvc.perform(multipart("/admin/gallery").file(file(png(), "a.png", "image/png"))
                        .param("category", "OTHER").header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.category").isNotEmpty());
        mvc.perform(multipart("/admin/gallery").param("category", "CLADDING")
                        .header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.file").isNotEmpty());
        mvc.perform(put("/admin/gallery/1").header("Authorization", token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"CLADDING\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.sortOrder").isNotEmpty());
    }

    @Test
    void deleteRemovesFileAndRowAndMissingIdsReturn404() throws Exception {
        long id = uploaded(png(), "delete.png", "image/png", null).get("id").asLong();
        GalleryItem item = repository.findById(id).orElseThrow();
        mvc.perform(delete("/admin/gallery/{id}", id).header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isNoContent());
        assertThat(repository.existsById(id)).isFalse();
        assertThat(uploads.resolve(item.getStorageKey())).doesNotExist();
        mvc.perform(get(item.getImageUrl())).andExpect(status().isNotFound());
        mvc.perform(delete("/admin/gallery/{id}", id).header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/admin/gallery/{id}", id).header("Authorization", token(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caption\":null,\"category\":\"PAINTING\",\"sortOrder\":0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicPermitIsGetOnlyAndLocalFilesAreReadOnly() throws Exception {
        mvc.perform(get("/api/gallery")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(post("/api/gallery")).andExpect(status().isForbidden());
        mvc.perform(post("/uploads/test.png")).andExpect(status().isForbidden());
        JsonNode image = uploaded(png(), "read-only.png", "image/png", null);
        mvc.perform(put(image.get("imageUrl").asText()).header("Authorization", token(Role.ADMIN)))
                .andExpect(status().isMethodNotAllowed());
    }

    private ResultActions upload(byte[] bytes, String filename, String contentType, String caption) throws Exception {
        var request = multipart("/admin/gallery").file(file(bytes, filename, contentType))
                .param("category", "CLADDING").header("Authorization", token(Role.ADMIN));
        if (caption != null) request.param("caption", caption);
        return mvc.perform(request);
    }

    private JsonNode uploaded(byte[] bytes, String filename, String contentType, String caption) throws Exception {
        return mapper.readTree(upload(bytes, filename, contentType, caption).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private MockMultipartFile file(byte[] bytes, String filename, String contentType) {
        return new MockMultipartFile("file", filename, contentType, bytes);
    }

    private String token(Role role) {
        return "Bearer " + jwt.generateToken(details.loadUserByUsername(
                "gallery-test-" + role.name().toLowerCase(java.util.Locale.ROOT)));
    }

    private byte[] png() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(12, 8, BufferedImage.TYPE_INT_ARGB), "png", output);
        return output.toByteArray();
    }

    private byte[] jpegWithExifGps(int width, int height) throws Exception {
        var jpeg = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpeg", jpeg);
        // Valid TIFF EXIF: GPS IFD pointer, latitude reference, and three rational coordinates.
        ByteBuffer tiff = ByteBuffer.allocate(80).order(ByteOrder.LITTLE_ENDIAN);
        tiff.put((byte) 'I').put((byte) 'I').putShort((short) 42).putInt(8);
        tiff.putShort((short) 1).putShort((short) 0x8825).putShort((short) 4).putInt(1).putInt(26).putInt(0);
        tiff.putShort((short) 2).putShort((short) 1).putShort((short) 2).putInt(2).putInt('N');
        tiff.putShort((short) 2).putShort((short) 5).putInt(3).putInt(56).putInt(0);
        tiff.putInt(32).putInt(1).putInt(0).putInt(1).putInt(0).putInt(1);
        var exif = new ByteArrayOutputStream();
        exif.write(new byte[]{'E', 'x', 'i', 'f', 0, 0});
        exif.write(tiff.array());
        byte[] bytes = jpeg.toByteArray();
        var output = new ByteArrayOutputStream();
        output.write(bytes, 0, 2);
        output.write(0xff); output.write(0xe1);
        int length = exif.size() + 2;
        output.write(length >> 8); output.write(length & 0xff);
        exif.writeTo(output);
        output.write(bytes, 2, bytes.length - 2);
        return output.toByteArray();
    }
}
