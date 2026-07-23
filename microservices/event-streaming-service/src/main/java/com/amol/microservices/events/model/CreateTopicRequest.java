package com.amol.microservices.events.model;

import jakarta.validation.constraints.NotBlank;

/** Request to create a topic. {@code partitions} is optional (defaults to the configured value). */
public record CreateTopicRequest(
        @NotBlank(message = "name is required") String name,
        Integer partitions) {
}
