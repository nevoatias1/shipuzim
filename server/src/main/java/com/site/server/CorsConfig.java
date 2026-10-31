package com.site.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.net.URI;
import org.springframework.util.Assert;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String allowedOrigin;

    public CorsConfig(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        Assert.hasText(allowedOrigin, "CORS_ALLOWED_ORIGIN is required");
        URI origin = URI.create(allowedOrigin);
        Assert.isTrue(("https".equals(origin.getScheme()) || "http".equals(origin.getScheme()))
                && origin.getHost() != null && origin.getRawUserInfo() == null
                && (origin.getRawPath() == null || origin.getRawPath().isEmpty())
                && origin.getRawQuery() == null && origin.getRawFragment() == null,
                "CORS_ALLOWED_ORIGIN must be a single HTTP(S) origin without a path");
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization");
    }
}
