package com.amol.microservices.orchestration.integration;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageRouterTest {

    private MessageRouter router;

    @BeforeEach
    void setUp() {
        OrchestrationProperties props = new OrchestrationProperties();
        props.getIntegration().setRoutes(Map.of(
                "loan", Map.of("disbursement", "payments-switch")));
        router = new MessageRouter(props, new ResilientDispatcher(props), new SimpleMeterRegistry());
    }

    @Test
    void routesToConfiguredDestination() {
        RoutingResult result = router.route(new CanonicalMessage("loan", "disbursement", Map.of()));
        assertThat(result.destination()).isEqualTo("payments-switch");
        assertThat(result.dispatched()).isTrue();
    }

    @Test
    void unknownRouteIsRejected() {
        assertThatThrownBy(() -> router.route(new CanonicalMessage("loan", "unknown-type", Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
