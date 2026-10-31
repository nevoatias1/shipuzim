package com.site.server.config;

import com.site.server.CorsConfig;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigTest {
    @Test
    void requiresAnExactNonBlankHttpOrigin() {
        for (String origin : new String[]{"", " ", "*", "https://*.example.com", "https://example.com/path",
                "https://user@example.com", "https://example.com?query=1", "https://example.com#fragment"}) {
            assertThatThrownBy(() -> new CorsConfig(origin)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatCode(() -> new CorsConfig("https://example.com")).doesNotThrowAnyException();
        assertThatCode(() -> new CorsConfig("http://localhost:5173")).doesNotThrowAnyException();
    }
}
