package com.amol.microservices.orchestration.integration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ESB / Integration API — content-based routing of a canonical message to its destination. */
@RestController
@RequestMapping("/integration")
@Tag(name = "ESB / Integration", description = "Content-based message routing with resilient dispatch")
public class IntegrationController {

    private final MessageRouter router;

    public IntegrationController(MessageRouter router) {
        this.router = router;
    }

    @PostMapping("/route")
    @Operation(summary = "Route a canonical message",
            description = "Resolves the destination from the message domain/type and dispatches it "
                    + "through the circuit-breaker-guarded, retrying dispatcher.")
    public ResponseEntity<RoutingResult> route(@Valid @RequestBody CanonicalMessage message) {
        return ResponseEntity.ok(router.route(message));
    }
}
