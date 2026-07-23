package com.amol.microservices.cbs.controller;

import com.amol.microservices.cbs.domain.Account;
import com.amol.microservices.cbs.domain.Customer;
import com.amol.microservices.cbs.domain.LedgerEntry;
import com.amol.microservices.cbs.model.AmountRequest;
import com.amol.microservices.cbs.model.CreateCustomerRequest;
import com.amol.microservices.cbs.model.OpenAccountRequest;
import com.amol.microservices.cbs.service.CoreBankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Core Banking API — customers, CASA accounts, deposits/withdrawals, and the general ledger. */
@RestController
@Tag(name = "Core Banking", description = "Customers, CASA accounts, and double-entry ledger")
public class CoreBankingController {

    private final CoreBankingService service;

    public CoreBankingController(CoreBankingService service) {
        this.service = service;
    }

    @PostMapping("/customers")
    @Operation(summary = "Onboard a customer")
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.ok(service.createCustomer(request.name(), request.email()));
    }

    @GetMapping("/customers/{id}")
    @Operation(summary = "Get a customer")
    public ResponseEntity<Customer> getCustomer(@PathVariable String id) {
        return ResponseEntity.ok(service.getCustomer(id));
    }

    @PostMapping("/accounts")
    @Operation(summary = "Open a CASA account")
    public ResponseEntity<Account> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        return ResponseEntity.ok(service.openAccount(request.customerId(), request.type(), request.currency()));
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "Get an account (with current balance)")
    public ResponseEntity<Account> getAccount(@PathVariable String id) {
        return ResponseEntity.ok(service.getAccount(id));
    }

    @PostMapping("/accounts/{id}/deposit")
    @Operation(summary = "Deposit into an account")
    public ResponseEntity<Account> deposit(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(service.deposit(id, request.amountMinor()));
    }

    @PostMapping("/accounts/{id}/withdraw")
    @Operation(summary = "Withdraw from an account")
    public ResponseEntity<Account> withdraw(@PathVariable String id, @Valid @RequestBody AmountRequest request) {
        return ResponseEntity.ok(service.withdraw(id, request.amountMinor()));
    }

    @GetMapping("/accounts/{id}/ledger")
    @Operation(summary = "Get the ledger postings for an account")
    public ResponseEntity<List<LedgerEntry>> ledger(@PathVariable String id) {
        return ResponseEntity.ok(service.ledgerFor(id));
    }
}
