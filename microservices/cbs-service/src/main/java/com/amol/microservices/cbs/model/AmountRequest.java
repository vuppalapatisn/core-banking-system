package com.amol.microservices.cbs.model;

import jakarta.validation.constraints.Positive;

/** A monetary amount in minor currency units (e.g. cents). */
public record AmountRequest(
        @Positive(message = "amountMinor must be positive") long amountMinor) {
}
