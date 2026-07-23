package com.amol.microservices.orchestration.workflow;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/** Request to start a workflow instance of {@code type} with the supplied business {@code facts}. */
public record StartWorkflowRequest(
        @NotBlank(message = "type is required") String type,
        Map<String, Double> facts) {
}
