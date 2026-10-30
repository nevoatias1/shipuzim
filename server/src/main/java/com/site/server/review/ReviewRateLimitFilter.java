package com.site.server.review;

import com.site.server.contact.ContactRateLimiter;
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
@Order(Ordered.LOWEST_PRECEDENCE - 9)
public class ReviewRateLimitFilter extends OncePerRequestFilter {
    private final ContactRateLimiter limiter;

    public ReviewRateLimitFilter(@Qualifier("reviewRateLimiter") ContactRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !("/users/review".equals(path) && ("PUT".equals(request.getMethod()) || "DELETE".equals(request.getMethod()))
                || path.startsWith("/admin/reviews/") && "DELETE".equals(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!limiter.allow(request.getRemoteAddr())) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"TOO_MANY_REQUESTS\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
