package com.site.server.gallery.storage;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "app.gallery.storage", havingValue = "cloudinary")
public class CloudinaryImageStorage implements ImageStorage {
    private final RestClient client;
    private final String apiKey;
    private final String apiSecret;

    @Autowired
    public CloudinaryImageStorage(@Value("${CLOUDINARY_CLOUD_NAME}") String cloudName,
                                  @Value("${CLOUDINARY_API_KEY}") String apiKey,
                                  @Value("${CLOUDINARY_API_SECRET}") String apiSecret) {
        this(clientFor(cloudName), apiKey, apiSecret);
    }

    CloudinaryImageStorage(RestClient client, String apiKey, String apiSecret) {
        Assert.hasText(apiKey, "CLOUDINARY_API_KEY is required");
        Assert.hasText(apiSecret, "CLOUDINARY_API_SECRET is required");
        this.client = client;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public StoredImage store(byte[] sanitizedPng) {
        String key = UUID.randomUUID().toString();
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        signedParameters(Map.of("public_id", key, "overwrite", "false")).forEach(parts::add);
        parts.add("file", new ByteArrayResource(sanitizedPng) {
            @Override
            public String getFilename() { return key + ".png"; }
        });
        try {
            JsonNode response = client.post().uri("/upload").contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts).retrieve().body(JsonNode.class);
            if (response == null || !key.equals(response.path("public_id").asText())
                    || !response.path("secure_url").asText().startsWith("https://")) {
                throw new ImageStorageException();
            }
            return new StoredImage(response.path("secure_url").asText(), key);
        } catch (RestClientException exception) {
            // Do not propagate upstream response bodies, credentials, or uploaded bytes into logs.
            throw new ImageStorageException();
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || !storageKey.matches("[a-f0-9-]{36}")) throw new ImageStorageException();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        signedParameters(Map.of("public_id", storageKey, "invalidate", "true")).forEach(parameters::add);
        try {
            JsonNode response = client.post().uri("/destroy").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(parameters).retrieve().body(JsonNode.class);
            String result = response == null ? "" : response.path("result").asText();
            if (!result.equals("ok") && !result.equals("not found")) throw new ImageStorageException();
        } catch (RestClientException exception) {
            throw new ImageStorageException();
        }
    }

    private Map<String, String> signedParameters(Map<String, String> values) {
        Map<String, String> parameters = new TreeMap<>(values);
        parameters.put("timestamp", Long.toString(Instant.now().getEpochSecond()));
        String serialized = parameters.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining("&"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((serialized + apiSecret).getBytes(StandardCharsets.UTF_8));
            parameters.put("signature", HexFormat.of().formatHex(digest));
            parameters.put("api_key", apiKey);
            return parameters;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }

    private static RestClient clientFor(String cloudName) {
        Assert.isTrue(cloudName != null && cloudName.matches("[A-Za-z0-9_-]+"),
                "CLOUDINARY_CLOUD_NAME is required and must be a valid cloud name");
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().baseUrl("https://api.cloudinary.com/v1_1/" + cloudName + "/image")
                .requestFactory(factory).build();
    }
}
