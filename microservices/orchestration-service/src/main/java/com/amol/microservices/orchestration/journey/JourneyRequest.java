package com.amol.microservices.orchestration.journey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Request to run the end-to-end loan journey (originate → book → disburse). */
public record JourneyRequest(
        @NotBlank(message = "applicantId is required") String applicantId,
        @Positive(message = "amountMinor must be positive") long amountMinor,
        @Positive(message = "termMonths must be positive") int termMonths,
        int creditScore,
        @PositiveOrZero(message = "annualRatePct must be >= 0") double annualRatePct,
        @NotBlank(message = "toAccount (borrower account) is required") String toAccount) {
}
