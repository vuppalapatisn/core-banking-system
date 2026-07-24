package com.amol.microservices.ai.model;

import jakarta.validation.constraints.PositiveOrZero;

/** Features for scoring a transaction's fraud risk. */
public record FraudRequest(
        @PositiveOrZero(message = "amountMinor must be >= 0") long amountMinor,
        String network,
        String country,
        String homeCountry,
        int recentTxnCount) {
}
