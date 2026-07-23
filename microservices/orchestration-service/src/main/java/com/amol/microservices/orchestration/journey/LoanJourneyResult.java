package com.amol.microservices.orchestration.journey;

/**
 * Outcome of the end-to-end loan journey. Fields after the application are null when the journey
 * stops early (e.g. the application was rejected in the LOS). {@code outcome} is DISBURSED or REJECTED.
 */
public record LoanJourneyResult(
        String applicationId,
        String applicationStatus,
        String loanId,
        Integer scheduleMonths,
        String paymentId,
        String paymentStatus,
        String outcome) {
}
