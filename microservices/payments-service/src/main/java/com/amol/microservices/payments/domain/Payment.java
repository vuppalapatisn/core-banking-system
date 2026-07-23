package com.amol.microservices.payments.domain;

/** A processed payment instruction and the network it was routed to. */
public record Payment(
        String id,
        String idempotencyKey,
        String fromAccount,
        String toAccount,
        long amountMinor,
        String currency,
        String network,
        PaymentStatus status) {
}
