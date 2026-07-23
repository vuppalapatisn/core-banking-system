package com.amol.microservices.orchestration.integration;

/**
 * A minimal circuit breaker — the resilience pattern a service mesh (e.g. Istio outlier detection)
 * would otherwise enforce at the infrastructure layer. Trips OPEN after {@code failureThreshold}
 * consecutive failures, stays open for {@code openMillis}, then allows a single HALF_OPEN trial.
 *
 * <p>Thread-safe; one instance per destination.
 */
public class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final int failureThreshold;
    private final long openMillis;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private long openedAtMillis = 0;

    public CircuitBreaker(int failureThreshold, long openMillis) {
        this.failureThreshold = failureThreshold;
        this.openMillis = openMillis;
    }

    /** @return true if a call may proceed now (also transitions OPEN → HALF_OPEN once the window elapses). */
    public synchronized boolean allowRequest() {
        if (state == State.OPEN && (now() - openedAtMillis) >= openMillis) {
            state = State.HALF_OPEN;
        }
        return state != State.OPEN;
    }

    public synchronized void onSuccess() {
        consecutiveFailures = 0;
        state = State.CLOSED;
    }

    public synchronized void onFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold || state == State.HALF_OPEN) {
            state = State.OPEN;
            openedAtMillis = now();
        }
    }

    public synchronized State state() {
        return state;
    }

    // Package-private seam so tests can control time without Thread.sleep.
    long now() {
        return System.currentTimeMillis();
    }
}
