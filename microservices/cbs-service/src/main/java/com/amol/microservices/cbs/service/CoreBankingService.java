package com.amol.microservices.cbs.service;

import com.amol.microservices.cbs.domain.Account;
import com.amol.microservices.cbs.domain.AccountType;
import com.amol.microservices.cbs.domain.Customer;
import com.amol.microservices.cbs.domain.EntryType;
import com.amol.microservices.cbs.domain.LedgerEntry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core banking system of record. Manages customers and CASA accounts and posts every money
 * movement to a double-entry general ledger (the account leg plus a contra cash leg), so the
 * ledger always balances. State is in-memory (a real CBS uses a durable, ACID store).
 */
@Service
public class CoreBankingService {

    private static final Logger log = LoggerFactory.getLogger(CoreBankingService.class);
    /** Contra account for the bank's cash position — the other leg of every customer posting. */
    private static final String GL_CASH = "GL-CASH";

    private final ConcurrentHashMap<String, Customer> customers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<LedgerEntry> ledger = new CopyOnWriteArrayList<>();
    private final MeterRegistry meterRegistry;

    public CoreBankingService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Customer createCustomer(String name, String email) {
        Customer customer = new Customer(UUID.randomUUID().toString(), name, email, "VERIFIED");
        customers.put(customer.id(), customer);
        log.info("customer_created id={}", customer.id());
        return customer;
    }

    public Customer getCustomer(String id) {
        Customer customer = customers.get(id);
        if (customer == null) {
            throw new IllegalArgumentException("Unknown customer: " + id);
        }
        return customer;
    }

    public Account openAccount(String customerId, AccountType type, String currency) {
        getCustomer(customerId); // validates existence
        Account account = new Account(UUID.randomUUID().toString(), customerId, type,
                currency == null || currency.isBlank() ? "USD" : currency);
        accounts.put(account.getId(), account);
        log.info("account_opened id={} customerId={} type={}", account.getId(), customerId, type);
        return account;
    }

    public Account getAccount(String id) {
        Account account = accounts.get(id);
        if (account == null) {
            throw new IllegalArgumentException("Unknown account: " + id);
        }
        return account;
    }

    public synchronized Account deposit(String accountId, long amountMinor) {
        requirePositive(amountMinor);
        Account account = getAccount(accountId);
        account.credit(amountMinor);
        post(account.getId(), EntryType.CREDIT, amountMinor, account.getBalanceMinor(), "deposit");
        post(GL_CASH, EntryType.DEBIT, amountMinor, 0, "deposit contra");
        counter("deposit").increment();
        return account;
    }

    public synchronized Account withdraw(String accountId, long amountMinor) {
        requirePositive(amountMinor);
        Account account = getAccount(accountId);
        if (account.getBalanceMinor() < amountMinor) {
            throw new IllegalStateException("Insufficient funds in account " + accountId);
        }
        account.debit(amountMinor);
        post(account.getId(), EntryType.DEBIT, amountMinor, account.getBalanceMinor(), "withdrawal");
        post(GL_CASH, EntryType.CREDIT, amountMinor, 0, "withdrawal contra");
        counter("withdrawal").increment();
        return account;
    }

    public List<LedgerEntry> ledgerFor(String accountId) {
        getAccount(accountId);
        return ledger.stream().filter(e -> e.accountId().equals(accountId)).toList();
    }

    private void post(String accountId, EntryType type, long amountMinor, long balanceAfter, String desc) {
        ledger.add(new LedgerEntry(UUID.randomUUID().toString(), accountId, type, amountMinor, balanceAfter, desc));
    }

    private static void requirePositive(long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private Counter counter(String type) {
        return Counter.builder("cbs.transactions")
                .tag("type", type)
                .description("Core banking transactions")
                .register(meterRegistry);
    }
}
