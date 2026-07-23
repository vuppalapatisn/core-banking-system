package com.amol.microservices.lms.domain;

/** One row of an amortization schedule (all money in minor units). */
public record Installment(
        int month,
        long emiMinor,
        long principalMinor,
        long interestMinor,
        long balanceMinor) {
}
