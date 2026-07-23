package com.amol.microservices.orchestration.integration;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatches to a destination with service-mesh-style resilience — bounded retries guarded by a
 * per-destination {@link CircuitBreaker}. Mirrors what Istio retry/timeout/outlier-detection would
 * provide at the mesh layer (see {@code k8s/orchestration-service/service-mesh}); implemented in code
 * so the behaviour holds even where no mesh is installed.
 */
@Component
public class ResilientDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ResilientDispatcher.class);

    /** A unit of work that sends the message downstream; may throw to signal failure. */
    @FunctionalInterface
    public interface DispatchAction {
        void run() throws Exception;
    }

    public enum DispatchOutcome { DISPATCHED, CIRCUIT_OPEN, FAILED }

    private final int maxRetries;
    private final int failureThreshold;
    private final long openMillis;
    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public ResilientDispatcher(OrchestrationProperties properties) {
        OrchestrationProperties.Resilience r = properties.getIntegration().getResilience();
        this.maxRetries = r.getMaxRetries();
        this.failureThreshold = r.getCircuitFailureThreshold();
        this.openMillis = r.getCircuitOpenMillis();
    }

    /** Attempts {@code action} for {@code destination}, honouring the circuit breaker and retries. */
    public DispatchOutcome dispatch(String destination, DispatchAction action) {
        CircuitBreaker breaker = breakers.computeIfAbsent(
                destination, d -> new CircuitBreaker(failureThreshold, openMillis));

        if (!breaker.allowRequest()) {
            log.warn("dispatch_skipped destination={} reason=circuit_open", destination);
            return DispatchOutcome.CIRCUIT_OPEN;
        }

        int attempts = maxRetries + 1;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                action.run();
                breaker.onSuccess();
                return DispatchOutcome.DISPATCHED;
            } catch (Exception ex) {
                breaker.onFailure();
                log.warn("dispatch_attempt_failed destination={} attempt={}/{} reason={}",
                        destination, attempt, attempts, ex.getMessage());
            }
        }
        return DispatchOutcome.FAILED;
    }

    /** Current breaker state for a destination (CLOSED if never used) — for diagnostics/tests. */
    public CircuitBreaker.State breakerState(String destination) {
        CircuitBreaker breaker = breakers.get(destination);
        return breaker == null ? CircuitBreaker.State.CLOSED : breaker.state();
    }
}
