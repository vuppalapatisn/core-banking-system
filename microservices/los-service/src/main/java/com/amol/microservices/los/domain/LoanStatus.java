package com.amol.microservices.los.domain;

/** Lifecycle of a loan application in the origination system. */
public enum LoanStatus {
    RECEIVED,
    APPROVED,
    REJECTED,
    ORIGINATED
}
