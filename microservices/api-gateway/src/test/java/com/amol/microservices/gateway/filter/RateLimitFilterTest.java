package com.amol.microservices.gateway.filter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void allowsUpToCapacityThenThrottles() {
        // capacity 3, no refill — deterministic.
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(3, 0.0);
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        // capacity 1, refill 10/sec (1 token per 100ms). Immediately after exhausting, the next
        // consume reliably fails; after a 250ms wait a token has been restored.
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(1, 10.0);
        assertThat(bucket.tryConsume()).isTrue();
        assertThat(bucket.tryConsume()).isFalse();
        Thread.sleep(250); // 250ms * 10/sec = 2.5 tokens, capped at capacity 1
        assertThat(bucket.tryConsume()).isTrue();
    }
}
