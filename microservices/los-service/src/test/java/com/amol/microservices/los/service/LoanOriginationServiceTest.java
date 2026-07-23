package com.amol.microservices.los.service;

import com.amol.microservices.los.domain.LoanApplication;
import com.amol.microservices.los.domain.LoanStatus;
import com.amol.microservices.los.events.EventPublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanOriginationServiceTest {

    private LoanOriginationService service;

    @BeforeEach
    void setUp() {
        service = new LoanOriginationService(new SimpleMeterRegistry(),
                new EventPublisher("http://unused", false, RestClient.builder()));
    }

    @Test
    void approvesGoodApplicationThenOriginates() {
        LoanApplication app = service.submit("cust-1", 1_000_000, 24, 720);
        assertThat(app.getStatus()).isEqualTo(LoanStatus.RECEIVED);

        service.underwrite(app.getId());
        assertThat(app.getStatus()).isEqualTo(LoanStatus.APPROVED);

        service.originate(app.getId());
        assertThat(app.getStatus()).isEqualTo(LoanStatus.ORIGINATED);
    }

    @Test
    void rejectsLowCreditScore() {
        LoanApplication app = service.submit("cust-2", 1_000_000, 24, 500);
        service.underwrite(app.getId());
        assertThat(app.getStatus()).isEqualTo(LoanStatus.REJECTED);
    }

    @Test
    void rejectsAmountOverLimit() {
        LoanApplication app = service.submit("cust-3", 9_000_000, 24, 800);
        service.underwrite(app.getId());
        assertThat(app.getStatus()).isEqualTo(LoanStatus.REJECTED);
    }

    @Test
    void cannotOriginateWithoutApproval() {
        LoanApplication app = service.submit("cust-4", 1_000_000, 24, 720);
        assertThatThrownBy(() -> service.originate(app.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submitValidatesAmount() {
        assertThatThrownBy(() -> service.submit("cust-5", 0, 24, 720))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
