package com.amol.microservices.ai.model;

import jakarta.validation.constraints.PositiveOrZero;

/** Features for customer segmentation and next-best-offer. */
public record SegmentRequest(
        @PositiveOrZero(message = "balanceMinor must be >= 0") long balanceMinor,
        int tenureMonths,
        int productCount,
        int monthlyTxns) {
}
