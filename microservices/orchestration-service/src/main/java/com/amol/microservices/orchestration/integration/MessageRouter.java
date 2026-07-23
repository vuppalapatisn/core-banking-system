package com.amol.microservices.orchestration.integration;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * ESB / integration backbone. Performs content-based routing: resolves a canonical message's
 * ({@code domain}, {@code type}) to a configured logical destination, then dispatches it through
 * the {@link ResilientDispatcher}. Routes are externalized ({@code orchestration.integration.routes}).
 */
@Service
public class MessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);

    private final OrchestrationProperties properties;
    private final ResilientDispatcher dispatcher;
    private final MeterRegistry meterRegistry;

    public MessageRouter(OrchestrationProperties properties, ResilientDispatcher dispatcher,
                         MeterRegistry meterRegistry) {
        this.properties = properties;
        this.dispatcher = dispatcher;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Routes {@code message} to its configured destination and dispatches it.
     *
     * @throws IllegalArgumentException if no route is configured for the domain/type
     */
    public RoutingResult route(CanonicalMessage message) {
        String destination = resolveDestination(message.domain(), message.type());

        // In this reference implementation the downstream send is simulated (no live backend);
        // the resilience wrapper still governs it exactly as a real network send would be governed.
        ResilientDispatcher.DispatchOutcome outcome =
                dispatcher.dispatch(destination, () -> simulateSend(destination, message));

        Counter.builder("orchestration.messages.routed")
                .tag("destination", destination)
                .tag("outcome", outcome.name())
                .description("Canonical messages routed by the ESB")
                .register(meterRegistry)
                .increment();

        boolean dispatched = outcome == ResilientDispatcher.DispatchOutcome.DISPATCHED;
        log.info("message_routed domain={} type={} destination={} outcome={}",
                message.domain(), message.type(), destination, outcome);
        return new RoutingResult(destination, dispatched, outcome.name().toLowerCase());
    }

    /** Resolves a destination or fails fast if the domain/type has no configured route. */
    public String resolveDestination(String domain, String type) {
        Map<String, String> domainRoutes = properties.getIntegration().getRoutes().get(domain);
        String destination = domainRoutes == null ? null : domainRoutes.get(type);
        if (destination == null) {
            throw new IllegalArgumentException("No route configured for " + domain + "/" + type);
        }
        return destination;
    }

    private void simulateSend(String destination, CanonicalMessage message) {
        // No-op stand-in for an outbound adapter (HTTP/JMS/Kafka). Never logs the payload (may hold PII).
        log.debug("simulate_send destination={} domain={} type={}",
                destination, message.domain(), message.type());
    }
}
