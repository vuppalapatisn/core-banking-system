package com.amol.microservices.orchestration.journey;

/**
 * Abstraction over the Core Platforms services the loan journey orchestrates (LOS, LMS, Payments).
 * Keeping this an interface lets the journey be unit-tested with a stub — no network required.
 */
public interface CorePlatformClient {

    /** Result of driving an application through LOS (submit → underwrite → originate). */
    record Origination(String applicationId, String status) {
    }

    /** Result of booking a loan in the LMS. */
    record Booking(String loanId, int scheduleMonths) {
    }

    /** Result of disbursing funds through the payments switch. */
    record Disbursement(String paymentId, String network, String status) {
    }

    Origination originateLoan(String applicantId, long amountMinor, int termMonths, int creditScore);

    Booking bookLoan(long principalMinor, double annualRatePct, int termMonths);

    Disbursement disburse(String fromAccount, String toAccount, long amountMinor, String idempotencyKey);
}
