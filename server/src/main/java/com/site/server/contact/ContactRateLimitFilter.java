package com.site.server.contact;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ContactRateLimitFilter extends OncePerRequestFilter {
    private final ContactRateLimiter limiter;
    private final ContactRateLimiter authenticationLimiter;

    public ContactRateLimitFilter(@Qualifier("contactRateLimiter") ContactRateLimiter limiter,
            @Qualifier("authenticationRateLimiter") ContactRateLimiter authenticationLimiter) {
        this.limiter = limiter;
        this.authenticationLimiter = authenticationLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod())
                || !("/api/contact".equals(path) || "/authenticate".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContactRateLimiter selected = request.getRequestURI().equals(request.getContextPath() + "/authenticate")
                ? authenticationLimiter : limiter;
        if (!selected.allow(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"TOO_MANY_REQUESTS\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
