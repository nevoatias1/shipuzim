package com.site.server.config;

import com.site.server.contact.ContactRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthenticationRateLimitConfig {
    @Bean
    public ContactRateLimiter authenticationRateLimiter(
            @Value("${app.auth.rate-limit.limit:10}") int limit,
            @Value("${app.auth.rate-limit.window-seconds:600}") long windowSeconds,
            @Value("${app.auth.rate-limit.max-ips:10000}") int maxIps) {
        return new ContactRateLimiter(limit, windowSeconds, maxIps);
    }
}
