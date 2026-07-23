package com.amol.microservices.orchestration.rules;

/** The result of a single rule firing against a set of facts. */
public record RuleMatch(String ruleName, String attribute, Outcome outcome, String message) {
}
