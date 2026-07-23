package com.amol.microservices.gateway.model;

import jakarta.validation.constraints.NotBlank;

/** Request to detokenize (decrypt) a previously issued token back to its original value. */
public record DetokenizeRequest(
        @NotBlank(message = "token is required") String token) {
}
