package com.amol.microservices.payments.controller;

import com.amol.microservices.payments.domain.Payment;
import com.amol.microservices.payments.model.PaymentRequest;
import com.amol.microservices.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Payments API — submit payments (idempotently) and look them up. */
@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Payment submission, routing, and idempotent processing")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Submit a payment",
            description = "Validates, routes to a network (INTERNAL / ACH / WIRE), and settles. "
                    + "Re-submitting the same idempotencyKey returns the original payment.")
    public ResponseEntity<Payment> submit(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(service.submit(
                request.idempotencyKey(), request.fromAccount(), request.toAccount(),
                request.amountMinor(), request.currency()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment")
    public ResponseEntity<Payment> get(@PathVariable String id) {
        return ResponseEntity.ok(service.get(id));
    }
}
