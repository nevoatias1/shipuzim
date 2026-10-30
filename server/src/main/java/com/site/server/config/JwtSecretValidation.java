package com.site.server.config;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretValidation {
    private final String secret;

    public JwtSecretValidation(@Value("${jwt.secret}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    public void validate() {
        try {
            if (Base64.getDecoder().decode(secret).length < 32) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET must be base64 encoding at least 32 bytes");
        }
    }
}
