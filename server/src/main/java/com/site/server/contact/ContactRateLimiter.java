package com.site.server.contact;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ContactRateLimiter {
    private final int limit;
    private final long windowNanos;
    private final int maxIps;
    private final LongSupplier ticker;
    private final Map<String, Window> windows = new HashMap<>();

    @Autowired
    public ContactRateLimiter(@Value("${app.contact.rate-limit.limit:5}") int limit,
                              @Value("${app.contact.rate-limit.window-seconds:600}") long windowSeconds,
                              @Value("${app.contact.rate-limit.max-ips:10000}") int maxIps) {
        this(limit, windowSeconds, maxIps, System::nanoTime);
    }

    ContactRateLimiter(int limit, long windowSeconds, int maxIps, LongSupplier ticker) {
        if (limit < 1 || windowSeconds < 1 || windowSeconds > Long.MAX_VALUE / 1_000_000_000L || maxIps < 1) {
            throw new IllegalArgumentException("Rate limit settings must be positive and within range");
        }
        this.limit = limit;
        this.windowNanos = windowSeconds * 1_000_000_000L;
        this.maxIps = maxIps;
        this.ticker = ticker;
    }

    public synchronized boolean allow(String ip) {
        long now = ticker.getAsLong();
        windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowNanos);
        Window window = windows.get(ip);
        if (window == null) {
            // Refuse new IPs when full rather than evicting an active limit.
            if (windows.size() >= maxIps) {
                return false;
            }
            windows.put(ip, new Window(now));
            return true;
        }
        if (window.count >= limit) {
            return false;
        }
        window.count++;
        return true;
    }

    private static class Window {
        private final long startedAt;
        private int count = 1;

        Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
