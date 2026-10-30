package com.site.server.config;

import java.util.Base64;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretValidationTest {
    @Test
    void rejectsMalformedOrShortKeysAndAccepts32DecodedBytes() {
        assertThatThrownBy(() -> new JwtSecretValidation("not base64!").validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtSecretValidation(Base64.getEncoder().encodeToString(new byte[31])).validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new JwtSecretValidation(Base64.getEncoder().encodeToString(new byte[32])).validate())
                .doesNotThrowAnyException();
    }
}
