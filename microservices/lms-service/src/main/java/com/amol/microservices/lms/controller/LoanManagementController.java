package com.amol.microservices.lms.controller;

import com.amol.microservices.lms.domain.Installment;
import com.amol.microservices.lms.domain.Loan;
import com.amol.microservices.lms.model.BookLoanRequest;
import com.amol.microservices.lms.model.PaymentRequest;
import com.amol.microservices.lms.service.LoanManagementService;
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

import java.util.List;

/** Loan Management API — book loans, view amortization schedules, and record repayments. */
@RestController
@RequestMapping("/loans")
@Tag(name = "Loan Management", description = "Loan servicing: schedules, repayments, payoff")
public class LoanManagementController {

    private final LoanManagementService service;

    public LoanManagementController(LoanManagementService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Book a loan for servicing")
    public ResponseEntity<Loan> book(@Valid @RequestBody BookLoanRequest request) {
        return ResponseEntity.ok(service.book(
                request.principalMinor(), request.annualRatePct(), request.termMonths()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a loan")
    public ResponseEntity<Loan> get(@PathVariable String id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Get the amortization schedule")
    public ResponseEntity<List<Installment>> schedule(@PathVariable String id) {
        return ResponseEntity.ok(service.schedule(id));
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Record a repayment")
    public ResponseEntity<Loan> pay(@PathVariable String id, @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(service.recordPayment(id, request.amountMinor()));
    }
}
