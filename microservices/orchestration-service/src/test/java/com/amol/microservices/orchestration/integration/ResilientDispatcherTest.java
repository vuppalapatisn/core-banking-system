package com.amol.microservices.orchestration.integration;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResilientDispatcherTest {

    private ResilientDispatcher dispatcherWith(int maxRetries, int threshold) {
        OrchestrationProperties props = new OrchestrationProperties();
        OrchestrationProperties.Resilience r = props.getIntegration().getResilience();
        r.setMaxRetries(maxRetries);
        r.setCircuitFailureThreshold(threshold);
        r.setCircuitOpenMillis(10_000);
        return new ResilientDispatcher(props);
    }

    @Test
    void dispatchesWhenActionSucceeds() {
        ResilientDispatcher dispatcher = dispatcherWith(2, 5);
        assertThat(dispatcher.dispatch("dest", () -> { /* success */ }))
                .isEqualTo(ResilientDispatcher.DispatchOutcome.DISPATCHED);
    }

    @Test
    void retriesThenFails() {
        ResilientDispatcher dispatcher = dispatcherWith(2, 99);
        int[] attempts = {0};
        ResilientDispatcher.DispatchOutcome outcome = dispatcher.dispatch("dest", () -> {
            attempts[0]++;
            throw new RuntimeException("boom");
        });
        assertThat(outcome).isEqualTo(ResilientDispatcher.DispatchOutcome.FAILED);
        assertThat(attempts[0]).isEqualTo(3); // 1 initial + 2 retries
    }

    @Test
    void opensCircuitAfterRepeatedFailures() {
        ResilientDispatcher dispatcher = dispatcherWith(0, 2); // 1 attempt/call, trip after 2 failures
        ResilientDispatcher.DispatchAction fail = () -> { throw new RuntimeException("boom"); };

        assertThat(dispatcher.dispatch("d", fail)).isEqualTo(ResilientDispatcher.DispatchOutcome.FAILED);
        assertThat(dispatcher.dispatch("d", fail)).isEqualTo(ResilientDispatcher.DispatchOutcome.FAILED);
        // Breaker now open — the next call is short-circuited without invoking the action.
        assertThat(dispatcher.dispatch("d", fail)).isEqualTo(ResilientDispatcher.DispatchOutcome.CIRCUIT_OPEN);
        assertThat(dispatcher.breakerState("d")).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
