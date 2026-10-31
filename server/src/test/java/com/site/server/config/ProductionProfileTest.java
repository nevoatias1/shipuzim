package com.site.server.config;

import com.site.server.CorsConfig;
import com.site.server.web.ClientIpResolver;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionProfileTest {
    private final ApplicationContextRunner production = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    void productionRequiresCorsAndJwtInsteadOfDevelopmentFallbacks() {
        production.withUserConfiguration(CorsConfig.class).run(context -> assertThat(context).hasFailed());
        production.withPropertyValues("CORS_ALLOWED_ORIGIN=https://example.invalid")
                .withUserConfiguration(CorsConfig.class, JwtSecretValidation.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void productionUsesExternalStorageAndDatabaseAndDisablesErrorDetails() {
        // Synthetic test-only key; never used to sign real tokens.
        production.withPropertyValues("CORS_ALLOWED_ORIGIN=https://example.invalid",
                        "PORT=10000",
                        "JWT_SECRET=" + Base64.getEncoder().encodeToString(new byte[32]))
                .withUserConfiguration(CorsConfig.class, JwtSecretValidation.class, ClientIpResolver.class).run(context -> {
                    assertThat(context).hasNotFailed();
                    var environment = context.getEnvironment();
                    assertThat(environment.getActiveProfiles()).containsExactly("prod");
                    assertThat(environment.getProperty("app.cors.allowed-origin")).isEqualTo("https://example.invalid");
                    assertThat(environment.getProperty("app.gallery.storage")).isEqualTo("cloudinary");
                    assertThat(environment.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
                    assertThat(environment.getProperty("server.port")).isEqualTo("10000");
                    assertThat(environment.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("5");
                    assertThat(environment.getProperty("spring.datasource.hikari.minimum-idle")).isEqualTo("0");
                    assertThat(environment.getProperty("spring.datasource.hikari.idle-timeout")).isEqualTo("60000");
                    assertThat(environment.getProperty("server.forward-headers-strategy")).isNull();
                    assertThat(environment.getProperty("app.client-ip-header")).isEqualTo("CF-Connecting-IP");
                    MockHttpServletRequest request = new MockHttpServletRequest();
                    request.setRemoteAddr("198.51.100.1");
                    request.addHeader("X-Forwarded-For", "192.0.2.1");
                    request.addHeader("CF-Connecting-IP", "203.0.113.1");
                    assertThat(context.getBean(ClientIpResolver.class).resolve(request)).isEqualTo("203.0.113.1");
                    for (String property : new String[]{"spring.datasource.url", "spring.datasource.username", "spring.datasource.password"}) {
                        assertThatThrownBy(() -> environment.getProperty(property)).isInstanceOf(IllegalArgumentException.class);
                    }
                    assertThat(environment.getProperty("spring.h2.console.enabled")).isEqualTo("false");
                    assertThat(environment.getProperty("app.mail.enabled")).isEqualTo("false");
                    assertThat(environment.getProperty("app.google.client-id")).isEmpty();
                    assertThat(environment.getProperty("ADMIN_USERNAME")).isNull();
                    assertThat(environment.getProperty("ADMIN_PASSWORD")).isNull();
                    assertThat(environment.getProperty("server.error.include-stacktrace")).isEqualTo("never");
                    assertThat(environment.getProperty("server.error.include-message")).isEqualTo("never");
                    assertThat(environment.getProperty("server.error.include-binding-errors")).isEqualTo("never");
                    assertThat(environment.getProperty("server.error.include-exception")).isEqualTo("false");
                });
    }

    @Test
    void enablingMailWithoutSettingsFailsStartup() {
        production.withPropertyValues("APP_MAIL_ENABLED=true").withUserConfiguration(SmtpConfig.class)
                .run(context -> assertThat(context).hasFailed());
    }
}
