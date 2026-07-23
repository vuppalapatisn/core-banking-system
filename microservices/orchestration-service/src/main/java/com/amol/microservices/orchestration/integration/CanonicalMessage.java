package com.amol.microservices.orchestration.integration;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * The canonical (normalized) message the ESB routes on. {@code domain} + {@code type} drive
 * content-based routing to a logical destination; {@code payload} is the opaque business content.
 */
public record CanonicalMessage(
        @NotBlank(message = "domain is required") String domain,
        @NotBlank(message = "type is required") String type,
        Map<String, Object> payload) {
}
