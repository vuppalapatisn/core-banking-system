package com.amol.microservices.gateway.model;

/**
 * Uniform error body returned by the gateway. Carries a stable machine-readable {@code error}
 * code and a safe human-readable {@code message} — never internal details, stack traces, or PII.
 */
public record ErrorResponse(String error, String message) {
}
