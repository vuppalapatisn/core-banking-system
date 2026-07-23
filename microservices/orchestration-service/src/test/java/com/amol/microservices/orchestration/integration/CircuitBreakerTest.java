package com.amol.microservices.orchestration.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerTest {

    /** Circuit breaker with a controllable clock so time-based transitions are deterministic. */
    private static final class TestClockBreaker extends CircuitBreaker {
        private long clock = 0;

        TestClockBreaker(int failureThreshold, long openMillis) {
            super(failureThreshold, openMillis);
        }

        void advanceClock(long millis) {
            clock += millis;
        }

        @Override
        long now() {
            return clock;
        }
    }

    @Test
    void tripsOpenAfterThresholdFailures() {
        TestClockBreaker cb = new TestClockBreaker(3, 10_000);
        assertThat(cb.allowRequest()).isTrue();
        cb.onFailure();
        cb.onFailure();
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.CLOSED);
        cb.onFailure(); // third consecutive failure trips it
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(cb.allowRequest()).isFalse();
    }

    @Test
    void halfOpensAfterWindowThenClosesOnSuccess() {
        TestClockBreaker cb = new TestClockBreaker(1, 10_000);
        cb.onFailure(); // threshold 1 -> OPEN immediately
        assertThat(cb.allowRequest()).isFalse();

        cb.advanceClock(10_000); // window elapsed
        assertThat(cb.allowRequest()).isTrue(); // transitions to HALF_OPEN
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        cb.onSuccess();
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void reopensWhenHalfOpenTrialFails() {
        TestClockBreaker cb = new TestClockBreaker(1, 5_000);
        cb.onFailure();
        cb.advanceClock(5_000);
        assertThat(cb.allowRequest()).isTrue(); // HALF_OPEN
        cb.onFailure(); // trial fails -> OPEN again
        assertThat(cb.state()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
