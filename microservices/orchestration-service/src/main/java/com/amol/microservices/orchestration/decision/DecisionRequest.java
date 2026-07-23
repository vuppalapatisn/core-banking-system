package com.amol.microservices.orchestration.decision;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * Request to orchestrate a decision. {@code ruleSets} is optional — when omitted the orchestrator
 * uses the default loan rule sets (credit + risk).
 */
public record DecisionRequest(
        @NotEmpty(message = "facts are required") Map<String, Double> facts,
        List<String> ruleSets) {
}
