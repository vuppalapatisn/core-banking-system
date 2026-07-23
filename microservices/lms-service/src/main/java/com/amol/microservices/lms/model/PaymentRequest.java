package com.amol.microservices.lms.model;

import jakarta.validation.constraints.Positive;

/** A repayment amount in minor currency units. */
public record PaymentRequest(
        @Positive(message = "amountMinor must be positive") long amountMinor) {
}
