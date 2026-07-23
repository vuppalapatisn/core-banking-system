package com.amol.microservices.cbs.domain;

/**
 * A CASA account. Balance is held in minor currency units (e.g. cents/paise) as a long to avoid
 * floating-point money errors.
 */
public class Account {

    private final String id;
    private final String customerId;
    private final AccountType type;
    private final String currency;
    private long balanceMinor;

    public Account(String id, String customerId, AccountType type, String currency) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.currency = currency;
        this.balanceMinor = 0L;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public AccountType getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceMinor() {
        return balanceMinor;
    }

    public void credit(long amountMinor) {
        balanceMinor += amountMinor;
    }

    public void debit(long amountMinor) {
        balanceMinor -= amountMinor;
    }
}
