package com.amol.microservices.orchestration.journey;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoanJourneyServiceTest {

    /** Deterministic stand-in for the Core Platforms — no network. */
    private static final class FakeClient implements CorePlatformClient {
        private final String originationStatus;

        FakeClient(String originationStatus) {
            this.originationStatus = originationStatus;
        }

        @Override
        public Origination originateLoan(String applicantId, long amountMinor, int termMonths, int creditScore) {
            return new Origination("app-1", originationStatus);
        }

        @Override
        public Booking bookLoan(long principalMinor, double annualRatePct, int termMonths) {
            return new Booking("loan-1", 12);
        }

        @Override
        public Disbursement disburse(String fromAccount, String toAccount, long amountMinor, String idempotencyKey) {
            return new Disbursement("pay-1", "INTERNAL", "SETTLED");
        }
    }

    private JourneyRequest request(int creditScore) {
        return new JourneyRequest("cust-1", 100_000, 12, creditScore, 12.0, "ACC-2");
    }

    @Test
    void originatedLoanIsBookedAndDisbursed() {
        LoanJourneyService service = new LoanJourneyService(new FakeClient("ORIGINATED"), new SimpleMeterRegistry());
        LoanJourneyResult result = service.run(request(720));

        assertThat(result.outcome()).isEqualTo("DISBURSED");
        assertThat(result.loanId()).isEqualTo("loan-1");
        assertThat(result.scheduleMonths()).isEqualTo(12);
        assertThat(result.paymentStatus()).isEqualTo("SETTLED");
    }

    @Test
    void rejectedApplicationStopsBeforeBooking() {
        LoanJourneyService service = new LoanJourneyService(new FakeClient("REJECTED"), new SimpleMeterRegistry());
        LoanJourneyResult result = service.run(request(400));

        assertThat(result.outcome()).isEqualTo("REJECTED");
        assertThat(result.loanId()).isNull();
        assertThat(result.paymentId()).isNull();
    }
}
