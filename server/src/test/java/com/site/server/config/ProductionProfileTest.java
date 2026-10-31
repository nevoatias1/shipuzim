package com.site.server.config;

import com.site.server.CorsConfig;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
                        "JWT_SECRET=" + Base64.getEncoder().encodeToString(new byte[32]))
                .withUserConfiguration(CorsConfig.class, JwtSecretValidation.class).run(context -> {
                    assertThat(context).hasNotFailed();
                    var environment = context.getEnvironment();
                    assertThat(environment.getActiveProfiles()).containsExactly("prod");
                    assertThat(environment.getProperty("app.cors.allowed-origin")).isEqualTo("https://example.invalid");
                    assertThat(environment.getProperty("app.gallery.storage")).isEqualTo("cloudinary");
                    assertThat(environment.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
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
