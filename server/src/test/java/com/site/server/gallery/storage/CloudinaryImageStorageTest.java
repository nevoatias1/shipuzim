package com.site.server.gallery.storage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudinaryImageStorageTest {
    @Test
    void uploadsSanitizedBytesWithUuidAndSha256Signature() throws Exception {
        var builder = RestClient.builder().baseUrl("https://cloudinary.example.test/image");
        var server = MockRestServiceServer.bindTo(builder).build();
        var storage = new CloudinaryImageStorage(builder.build(), "test-api-key", "test-api-secret");
        server.expect(requestTo("https://cloudinary.example.test/image/upload")).andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                    String id = part(body, "public_id");
                    String timestamp = part(body, "timestamp");
                    assertThat(id).matches("[a-f0-9-]{36}");
                    assertThat(part(body, "overwrite")).isEqualTo("false");
                    assertThat(part(body, "api_key")).isEqualTo("test-api-key");
                    assertThat(part(body, "signature")).isEqualTo(hash(
                            "overwrite=false&public_id=" + id + "&timestamp=" + timestamp + "test-api-secret"));
                    assertThat(body).contains("filename=\"" + id + ".png\"", "sanitized-pixels")
                            .doesNotContain("test-api-secret", "caption");
                    return withSuccess("{\"public_id\":\"" + id + "\",\"secure_url\":\"https://res.cloudinary.com/test/image/upload/" + id + ".png\"}",
                            MediaType.APPLICATION_JSON).createResponse(request);
                });
        var result = storage.store("sanitized-pixels".getBytes(StandardCharsets.UTF_8));
        assertThat(result.imageUrl()).endsWith(result.storageKey() + ".png");
        server.verify();
    }

    @Test
    void deletionIsSignedAndInvalidatesCdnCopies() {
        var builder = RestClient.builder().baseUrl("https://cloudinary.example.test/image");
        var server = MockRestServiceServer.bindTo(builder).build();
        var storage = new CloudinaryImageStorage(builder.build(), "test-api-key", "test-api-secret");
        String key = "00000000-0000-0000-0000-000000000001";
        server.expect(requestTo("https://cloudinary.example.test/image/destroy")).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("invalidate=true")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("public_id=" + key)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("signature=")))
                .andRespond(withSuccess("{\"result\":\"not found\"}", MediaType.APPLICATION_JSON));
        storage.delete(key);
        server.verify();
    }

    @Test
    void upstreamFailuresExposeOnlyAGenericStorageError() {
        var builder = RestClient.builder().baseUrl("https://cloudinary.example.test/image");
        var server = MockRestServiceServer.bindTo(builder).build();
        var storage = new CloudinaryImageStorage(builder.build(), "test-api-key", "test-api-secret");
        server.expect(requestTo("https://cloudinary.example.test/image/upload"))
                .andRespond(withServerError().body("private-upstream-response"));
        assertThatThrownBy(() -> storage.store(new byte[]{1, 2, 3}))
                .isInstanceOf(ImageStorageException.class)
                .hasMessage("Image storage operation failed.").hasNoCause();
        server.verify();
    }

    private static String part(String body, String name) {
        var matcher = Pattern.compile("name=\"" + name + "\"[^\\r]*\\r\\n(?:[^\\r]+\\r\\n)*\\r\\n([^\\r]+)")
                .matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
