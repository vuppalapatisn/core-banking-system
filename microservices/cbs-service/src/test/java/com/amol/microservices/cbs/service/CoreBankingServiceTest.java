package com.amol.microservices.cbs.service;

import com.amol.microservices.cbs.domain.Account;
import com.amol.microservices.cbs.domain.AccountType;
import com.amol.microservices.cbs.domain.Customer;
import com.amol.microservices.cbs.domain.LedgerEntry;
import com.amol.microservices.cbs.events.EventPublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoreBankingServiceTest {

    private CoreBankingService service;
    private String customerId;

    @BeforeEach
    void setUp() {
        service = new CoreBankingService(new SimpleMeterRegistry(),
                new EventPublisher("http://unused", false, RestClient.builder()));
        customerId = service.createCustomer("Ada Lovelace", "ada@example.com").id();
    }

    @Test
    void opensAccountWithZeroBalance() {
        Account account = service.openAccount(customerId, AccountType.SAVINGS, "USD");
        assertThat(account.getBalanceMinor()).isZero();
        assertThat(account.getType()).isEqualTo(AccountType.SAVINGS);
    }

    @Test
    void openAccountRejectsUnknownCustomer() {
        assertThatThrownBy(() -> service.openAccount("nope", AccountType.CURRENT, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void depositThenWithdrawUpdatesBalanceAndLedger() {
        Account account = service.openAccount(customerId, AccountType.CURRENT, "USD");
        service.deposit(account.getId(), 10_000);
        assertThat(service.getAccount(account.getId()).getBalanceMinor()).isEqualTo(10_000);

        service.withdraw(account.getId(), 4_000);
        assertThat(service.getAccount(account.getId()).getBalanceMinor()).isEqualTo(6_000);

        List<LedgerEntry> ledger = service.ledgerFor(account.getId());
        assertThat(ledger).hasSize(2);
        assertThat(ledger.get(0).balanceAfterMinor()).isEqualTo(10_000);
        assertThat(ledger.get(1).balanceAfterMinor()).isEqualTo(6_000);
    }

    @Test
    void withdrawRejectsInsufficientFunds() {
        Account account = service.openAccount(customerId, AccountType.CURRENT, "USD");
        service.deposit(account.getId(), 1_000);
        assertThatThrownBy(() -> service.withdraw(account.getId(), 5_000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void depositRejectsNonPositiveAmount() {
        Account account = service.openAccount(customerId, AccountType.CURRENT, "USD");
        assertThatThrownBy(() -> service.deposit(account.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createdCustomerIsVerified() {
        Customer c = service.getCustomer(customerId);
        assertThat(c.kycStatus()).isEqualTo("VERIFIED");
    }
}
