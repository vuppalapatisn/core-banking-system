package com.amol.microservices.cbs.domain;

/** An immutable general-ledger posting against an account, with the running balance after it. */
public record LedgerEntry(
        String id,
        String accountId,
        EntryType entryType,
        long amountMinor,
        long balanceAfterMinor,
        String description) {
}
