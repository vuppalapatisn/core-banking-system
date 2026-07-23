package com.amol.microservices.payments.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Request to submit a payment. {@code idempotencyKey} is optional but recommended for safe retries. */
public record PaymentRequest(
        String idempotencyKey,
        @NotBlank(message = "fromAccount is required") String fromAccount,
        @NotBlank(message = "toAccount is required") String toAccount,
        @Positive(message = "amountMinor must be positive") long amountMinor,
        String currency) {
}
