package com.amol.microservices.los.controller;

import com.amol.microservices.los.domain.LoanApplication;
import com.amol.microservices.los.model.SubmitApplicationRequest;
import com.amol.microservices.los.service.LoanOriginationService;
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

/** Loan Origination API — submit, underwrite, and originate loan applications. */
@RestController
@RequestMapping("/applications")
@Tag(name = "Loan Origination", description = "Loan application intake, underwriting, and origination")
public class LoanOriginationController {

    private final LoanOriginationService service;

    public LoanOriginationController(LoanOriginationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Submit a loan application")
    public ResponseEntity<LoanApplication> submit(@Valid @RequestBody SubmitApplicationRequest request) {
        return ResponseEntity.ok(service.submit(
                request.applicantId(), request.amountMinor(), request.termMonths(), request.creditScore()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a loan application")
    public ResponseEntity<LoanApplication> get(@PathVariable String id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/{id}/underwrite")
    @Operation(summary = "Underwrite an application (APPROVED / REJECTED)")
    public ResponseEntity<LoanApplication> underwrite(@PathVariable String id) {
        return ResponseEntity.ok(service.underwrite(id));
    }

    @PostMapping("/{id}/originate")
    @Operation(summary = "Originate (book) an approved application")
    public ResponseEntity<LoanApplication> originate(@PathVariable String id) {
        return ResponseEntity.ok(service.originate(id));
    }
}
