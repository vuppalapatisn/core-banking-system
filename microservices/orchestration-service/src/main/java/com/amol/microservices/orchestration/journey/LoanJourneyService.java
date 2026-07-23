package com.amol.microservices.orchestration.journey;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the end-to-end loan journey across the Core Platforms:
 * <ol>
 *   <li>LOS — submit, underwrite, and (if approved) originate the application;</li>
 *   <li>LMS — book the originated loan and fetch its amortization schedule;</li>
 *   <li>Payments — disburse the funds to the borrower.</li>
 * </ol>
 * If the LOS does not originate the loan, the journey stops with outcome REJECTED.
 */
@Service
public class LoanJourneyService {

    private static final Logger log = LoggerFactory.getLogger(LoanJourneyService.class);
    private static final String DISBURSEMENT_SOURCE = "BANK-DISBURSEMENT";

    private final CorePlatformClient client;
    private final MeterRegistry meterRegistry;

    public LoanJourneyService(CorePlatformClient client, MeterRegistry meterRegistry) {
        this.client = client;
        this.meterRegistry = meterRegistry;
    }

    public LoanJourneyResult run(JourneyRequest request) {
        CorePlatformClient.Origination origination = client.originateLoan(
                request.applicantId(), request.amountMinor(), request.termMonths(), request.creditScore());

        if (!"ORIGINATED".equals(origination.status())) {
            counter("rejected").increment();
            log.info("journey_rejected applicationId={} status={}",
                    origination.applicationId(), origination.status());
            return new LoanJourneyResult(origination.applicationId(), origination.status(),
                    null, null, null, null, "REJECTED");
        }

        CorePlatformClient.Booking booking = client.bookLoan(
                request.amountMinor(), request.annualRatePct(), request.termMonths());

        CorePlatformClient.Disbursement disbursement = client.disburse(
                DISBURSEMENT_SOURCE, request.toAccount(), request.amountMinor(), booking.loanId());

        counter("disbursed").increment();
        log.info("journey_disbursed applicationId={} loanId={} paymentId={}",
                origination.applicationId(), booking.loanId(), disbursement.paymentId());
        return new LoanJourneyResult(
                origination.applicationId(), origination.status(),
                booking.loanId(), booking.scheduleMonths(),
                disbursement.paymentId(), disbursement.status(), "DISBURSED");
    }

    private Counter counter(String outcome) {
        return Counter.builder("orchestration.journeys")
                .tag("outcome", outcome)
                .description("End-to-end loan journeys")
                .register(meterRegistry);
    }
}
