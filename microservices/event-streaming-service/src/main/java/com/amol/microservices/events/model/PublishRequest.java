package com.amol.microservices.events.model;

import jakarta.validation.constraints.NotNull;

/** Request to publish an event. {@code key} is optional; when present it pins the partition. */
public record PublishRequest(
        String key,
        @NotNull(message = "payload is required") Object payload) {
}
