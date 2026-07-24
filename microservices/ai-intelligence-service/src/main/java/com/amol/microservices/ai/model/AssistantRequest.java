package com.amol.microservices.ai.model;

import jakarta.validation.constraints.NotBlank;

/** A question for the GenAI assistant, with optional grounding context. */
public record AssistantRequest(
        @NotBlank(message = "question is required") String question,
        String context) {
}
