package com.amol.microservices.lms.service;

import com.amol.microservices.lms.domain.Installment;
import com.amol.microservices.lms.domain.Loan;
import com.amol.microservices.lms.domain.LoanState;
import com.amol.microservices.lms.events.EventPublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanManagementServiceTest {

    private LoanManagementService service;

    @BeforeEach
    void setUp() {
        service = new LoanManagementService(new SimpleMeterRegistry(),
                new EventPublisher("http://unused", false, RestClient.builder()));
    }

    @Test
    void computesEmiForKnownLoan() {
        // 1000.00 at 12% for 12 months -> EMI ~ 88.85 (8885 minor).
        Loan loan = service.book(100_000, 12.0, 12);
        assertThat(service.monthlyInstalment(loan)).isBetween(8_800L, 8_900L);
    }

    @Test
    void zeroInterestSplitsPrincipalEvenly() {
        Loan loan = service.book(120_000, 0.0, 12);
        assertThat(service.monthlyInstalment(loan)).isEqualTo(10_000);
    }

    @Test
    void scheduleCoversTermAndEndsAtZero() {
        Loan loan = service.book(100_000, 12.0, 12);
        List<Installment> schedule = service.schedule(loan.getId());
        assertThat(schedule).hasSize(12);
        assertThat(schedule.get(schedule.size() - 1).balanceMinor()).isZero();
    }

    @Test
    void repaymentReducesOutstandingAndPaysOff() {
        Loan loan = service.book(100_000, 12.0, 12);
        service.recordPayment(loan.getId(), 40_000);
        assertThat(service.get(loan.getId()).getOutstandingMinor()).isEqualTo(60_000);
        assertThat(service.get(loan.getId()).getState()).isEqualTo(LoanState.ACTIVE);

        service.recordPayment(loan.getId(), 60_000);
        assertThat(service.get(loan.getId()).getOutstandingMinor()).isZero();
        assertThat(service.get(loan.getId()).getState()).isEqualTo(LoanState.PAID_OFF);
    }

    @Test
    void cannotPayOffTwice() {
        Loan loan = service.book(100_000, 12.0, 12);
        service.recordPayment(loan.getId(), 100_000);
        assertThatThrownBy(() -> service.recordPayment(loan.getId(), 1_000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bookValidatesPrincipal() {
        assertThatThrownBy(() -> service.book(0, 12.0, 12))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
