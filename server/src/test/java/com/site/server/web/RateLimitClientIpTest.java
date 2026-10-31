package com.site.server.web;

import com.site.server.contact.ContactRateLimitFilter;
import com.site.server.contact.ContactRateLimiter;
import com.site.server.review.ReviewRateLimitFilter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimitClientIpTest {
    @ParameterizedTest
    @ValueSource(strings = {"/api/contact", "/authenticate", "/auth/google", "/users/review", "/admin/reviews/1"})
    void everyRateLimitBudgetUsesTheConfiguredClientIp(String path) throws Exception {
        OncePerRequestFilter filter = filter(path);
        assertThat(send(filter, path, "198.51.100.1", "203.0.113.1", "192.0.2.1")).isEqualTo(204);
        assertThat(send(filter, path, "198.51.100.2", "203.0.113.1", "192.0.2.2")).isEqualTo(429);
        assertThat(send(filter, path, "198.51.100.1", "203.0.113.2", "192.0.2.1")).isEqualTo(204);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/contact", "/authenticate", "/auth/google", "/users/review", "/admin/reviews/1"})
    void invalidOrMissingTrustedHeaderCannotBypassLimitsWithForgedForwardedFor(String path) throws Exception {
        OncePerRequestFilter filter = filter(path);
        assertThat(send(filter, path, "198.51.100.1", null, "192.0.2.1")).isEqualTo(204);
        assertThat(send(filter, path, "198.51.100.1", "not-an-ip", "192.0.2.2")).isEqualTo(429);
    }

    private OncePerRequestFilter filter(String path) {
        ClientIpResolver resolver = new ClientIpResolver("CF-Connecting-IP");
        return path.contains("review")
                ? new ReviewRateLimitFilter(new ContactRateLimiter(1, 600, 100), resolver)
                : new ContactRateLimitFilter(new ContactRateLimiter(1, 600, 100),
                        new ContactRateLimiter(1, 600, 100), resolver);
    }

    private int send(OncePerRequestFilter filter, String path, String remote, String trustedIp, String forgedIp)
            throws Exception {
        String method = path.startsWith("/admin/") ? "DELETE" : path.equals("/users/review") ? "PUT" : "POST";
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remote);
        if (trustedIp != null) request.addHeader("CF-Connecting-IP", trustedIp);
        request.addHeader("X-Forwarded-For", forgedIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignored, result) -> ((MockHttpServletResponse) result).setStatus(204));
        return response.getStatus();
    }
}
