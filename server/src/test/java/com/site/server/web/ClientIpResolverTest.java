package com.site.server.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {
    private final ClientIpResolver resolver = new ClientIpResolver("CF-Connecting-IP");

    @Test
    void unconfiguredResolverIgnoresAllForwardingHeaders() {
        MockHttpServletRequest request = request();
        request.addHeader("CF-Connecting-IP", "203.0.113.1");
        assertThat(new ClientIpResolver("").resolve(request)).isEqualTo("198.51.100.1");
        assertThat(new ClientIpResolver(" ").resolve(request)).isEqualTo("198.51.100.1");
    }

    @Test
    void missingConfiguredHeaderIgnoresForgedForwardedFor() {
        assertThat(resolver.resolve(request())).isEqualTo("198.51.100.1");
    }

    @Test
    void usesOneConfiguredLiteralAndNormalizesEquivalentIpv6Addresses() {
        MockHttpServletRequest request = request();
        request.addHeader("CF-Connecting-IP", " 203.0.113.2 ");
        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.2");
        request.removeHeader("CF-Connecting-IP");
        request.addHeader("CF-Connecting-IP", "2001:db8::1");
        String canonical = resolver.resolve(request);
        request.removeHeader("CF-Connecting-IP");
        request.addHeader("CF-Connecting-IP", "2001:0DB8:0:0:0:0:0:1");
        assertThat(resolver.resolve(request)).isEqualTo(canonical).isEqualTo("2001:db8:0:0:0:0:0:1");
        request.removeHeader("CF-Connecting-IP");
        request.addHeader("CF-Connecting-IP", "::ffff:203.0.113.2");
        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "example.com", "localhost", "999.0.0.1", "127.1", "2130706433",
            "0x7f000001", "010.001.002.003", "203.0.113.1, 203.0.113.2", "203.0.113.1:443",
            "[2001:db8::1]", "fe80::1%eth0", "2001:db8::gg", ":::", "2001:db8::192.0.2.999",
            "203.0.113.1\nspoof", "2001:db8:0000:0000:0000:0000:0000:0000:0000:0000:0001"})
    void invalidHeaderValuesFallBackToSocketAddress(String header) {
        MockHttpServletRequest request = request();
        request.addHeader("CF-Connecting-IP", header);
        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.1");
    }

    @Test
    void duplicateHeaderLinesFallBackEvenWhenBothValuesAreValid() {
        MockHttpServletRequest request = request();
        request.addHeader("CF-Connecting-IP", "203.0.113.1");
        request.addHeader("CF-Connecting-IP", "203.0.113.2");
        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.1");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.1");
        request.addHeader("X-Forwarded-For", "192.0.2.200, 192.0.2.201");
        request.addHeader("Forwarded", "for=192.0.2.200");
        return request;
    }
}
