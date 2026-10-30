package com.site.server.contact;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContactRateLimiterTest {
    @Test
    void expiredWindowsAreEvictedAndTheIpGetsANewAllowance() {
        AtomicLong ticker = new AtomicLong();
        ContactRateLimiter limiter = new ContactRateLimiter(1, 1, 1, ticker::get);
        assertThat(limiter.allow("192.0.2.1")).isTrue();
        assertThat(limiter.allow("192.0.2.1")).isFalse();
        ticker.set(1_000_000_000L);
        assertThat(limiter.allow("192.0.2.2")).isTrue();
        ticker.set(2_000_000_000L);
        assertThat(limiter.allow("192.0.2.1")).isTrue();
    }

    @Test
    void capacityDoesNotEvictActiveLimitsToAdmitNewIps() {
        ContactRateLimiter limiter = new ContactRateLimiter(2, 600, 1);
        assertThat(limiter.allow("192.0.2.1")).isTrue();
        assertThat(limiter.allow("192.0.2.2")).isFalse();
        assertThat(limiter.allow("192.0.2.1")).isTrue();
        assertThat(limiter.allow("192.0.2.1")).isFalse();
    }
}
