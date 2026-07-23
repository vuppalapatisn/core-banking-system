package com.amol.microservices.events.model;

import jakarta.validation.constraints.NotBlank;

/**
 * A change-data-capture record. Published to the {@code cdc.<entity>} topic, keyed by {@code key}
 * so all changes for one entity instance stay ordered on the same partition.
 */
public record CdcRequest(
        @NotBlank(message = "entity is required") String entity,
        @NotBlank(message = "changeType is required (INSERT/UPDATE/DELETE)") String changeType,
        String key,
        Object before,
        Object after) {
}
