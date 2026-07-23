package com.amol.microservices.payments.service;

import com.amol.microservices.payments.domain.Payment;
import com.amol.microservices.payments.domain.PaymentStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(new SimpleMeterRegistry());
    }

    @Test
    void submitsAndSettles() {
        Payment p = service.submit("k1", "ACC-1", "ACC-2", 5_000, "USD");
        assertThat(p.status()).isEqualTo(PaymentStatus.SETTLED);
        assertThat(p.network()).isEqualTo("INTERNAL");
    }

    @Test
    void routesLargeAmountToWire() {
        assertThat(service.chooseNetwork("ACC-2", 2_000_000)).isEqualTo("WIRE");
    }

    @Test
    void routesExternalAccountToAch() {
        assertThat(service.chooseNetwork("EXT-9", 5_000)).isEqualTo("ACH");
    }

    @Test
    void idempotentKeyReturnsSamePayment() {
        Payment first = service.submit("dup", "ACC-1", "ACC-2", 5_000, "USD");
        Payment second = service.submit("dup", "ACC-1", "ACC-2", 5_000, "USD");
        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    void rejectsSameFromAndTo() {
        assertThatThrownBy(() -> service.submit("k", "ACC-1", "ACC-1", 5_000, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.submit("k", "ACC-1", "ACC-2", 0, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
