package com.amol.microservices.los.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Request to submit a loan application. */
public record SubmitApplicationRequest(
        @NotBlank(message = "applicantId is required") String applicantId,
        @Positive(message = "amountMinor must be positive") long amountMinor,
        @Positive(message = "termMonths must be positive") int termMonths,
        int creditScore) {
}
