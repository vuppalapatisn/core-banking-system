package com.amol.microservices.lms.model;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Request to book a loan for servicing. */
public record BookLoanRequest(
        @Positive(message = "principalMinor must be positive") long principalMinor,
        @PositiveOrZero(message = "annualRatePct must be >= 0") double annualRatePct,
        @Positive(message = "termMonths must be positive") int termMonths) {
}
