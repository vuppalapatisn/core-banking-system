package com.amol.microservices.ai.model;

import jakarta.validation.constraints.PositiveOrZero;

/** Features for computing a credit score. */
public record CreditRequest(
        @PositiveOrZero(message = "annualIncomeMinor must be >= 0") long annualIncomeMinor,
        @PositiveOrZero(message = "monthlyDebtMinor must be >= 0") long monthlyDebtMinor,
        int ageYears,
        int delinquencies,
        double creditUtilizationPct) {
}
