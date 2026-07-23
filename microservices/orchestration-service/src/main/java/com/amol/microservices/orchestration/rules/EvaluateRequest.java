package com.amol.microservices.orchestration.rules;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/** Request to evaluate a named rule set against a set of numeric facts. */
public record EvaluateRequest(
        @NotBlank(message = "ruleSet is required") String ruleSet,
        Map<String, Double> facts) {
}
