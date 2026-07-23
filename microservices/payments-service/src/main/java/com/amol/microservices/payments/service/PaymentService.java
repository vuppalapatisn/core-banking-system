package com.amol.microservices.payments.service;

import com.amol.microservices.payments.domain.Payment;
import com.amol.microservices.payments.domain.PaymentStatus;
import com.amol.microservices.payments.events.EventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Payments switch. Validates and routes a payment to the appropriate network, then records it.
 * Processing is idempotent: re-submitting the same {@code idempotencyKey} returns the original
 * payment instead of creating a duplicate. State is in-memory (a real switch persists durably).
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final long WIRE_THRESHOLD_MINOR = 1_000_000; // 10,000.00

    private final ConcurrentHashMap<String, Payment> paymentsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Payment> paymentsByKey = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;
    private final EventPublisher events;

    public PaymentService(MeterRegistry meterRegistry, EventPublisher events) {
        this.meterRegistry = meterRegistry;
        this.events = events;
    }

    public Payment submit(String idempotencyKey, String fromAccount, String toAccount,
                          long amountMinor, String currency) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("amountMinor must be positive");
        }
        if (fromAccount == null || toAccount == null || fromAccount.isBlank() || toAccount.isBlank()) {
            throw new IllegalArgumentException("fromAccount and toAccount are required");
        }
        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("fromAccount and toAccount must differ");
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Payment existing = paymentsByKey.get(idempotencyKey);
            if (existing != null) {
                log.info("payment_idempotent_hit key={} id={}", idempotencyKey, existing.id());
                return existing;
            }
        }

        String network = chooseNetwork(toAccount, amountMinor);
        Payment payment = new Payment(
                UUID.randomUUID().toString(), idempotencyKey, fromAccount, toAccount,
                amountMinor, currency == null || currency.isBlank() ? "USD" : currency,
                network, PaymentStatus.SETTLED);
        paymentsById.put(payment.id(), payment);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            paymentsByKey.putIfAbsent(idempotencyKey, payment);
        }
        Counter.builder("payments.processed").tag("network", network).register(meterRegistry).increment();
        events.publish("payments.payment", payment.id(), Map.of(
                "type", "SETTLED", "paymentId", payment.id(), "network", network, "amountMinor", amountMinor));
        log.info("payment_processed id={} network={} status={}", payment.id(), network, payment.status());
        return payment;
    }

    public Payment get(String id) {
        Payment payment = paymentsById.get(id);
        if (payment == null) {
            throw new IllegalArgumentException("Unknown payment: " + id);
        }
        return payment;
    }

    /** Content-based network routing: large amounts go by wire, external accounts by ACH, else internal. */
    public String chooseNetwork(String toAccount, long amountMinor) {
        if (amountMinor > WIRE_THRESHOLD_MINOR) {
            return "WIRE";
        }
        if (toAccount.startsWith("EXT")) {
            return "ACH";
        }
        return "INTERNAL";
    }
}
