package com.site.server.config;

import com.site.server.contact.ContactRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReviewRateLimitConfig {
    @Bean
    public ContactRateLimiter reviewRateLimiter(
            @Value("${app.reviews.rate-limit.limit:5}") int limit,
            @Value("${app.reviews.rate-limit.window-seconds:3600}") long seconds,
            @Value("${app.reviews.rate-limit.max-ips:10000}") int maxIps) {
        return new ContactRateLimiter(limit, seconds, maxIps);
    }
}
