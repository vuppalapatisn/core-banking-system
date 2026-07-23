package com.amol.microservices.gateway.model;

import jakarta.validation.constraints.NotBlank;

/** Request to tokenize (encrypt) a sensitive value such as a card or account number. */
public record TokenizeRequest(
        @NotBlank(message = "value is required") String value) {
}
