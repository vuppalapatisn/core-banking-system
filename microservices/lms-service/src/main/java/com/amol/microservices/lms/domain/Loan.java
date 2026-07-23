package com.amol.microservices.lms.domain;

/** A booked loan being serviced. Balances are in minor currency units. */
public class Loan {

    private final String id;
    private final long principalMinor;
    private final double annualRatePct;
    private final int termMonths;
    private long outstandingMinor;
    private LoanState state;

    public Loan(String id, long principalMinor, double annualRatePct, int termMonths) {
        this.id = id;
        this.principalMinor = principalMinor;
        this.annualRatePct = annualRatePct;
        this.termMonths = termMonths;
        this.outstandingMinor = principalMinor;
        this.state = LoanState.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public long getPrincipalMinor() {
        return principalMinor;
    }

    public double getAnnualRatePct() {
        return annualRatePct;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public long getOutstandingMinor() {
        return outstandingMinor;
    }

    public void setOutstandingMinor(long outstandingMinor) {
        this.outstandingMinor = outstandingMinor;
    }

    public LoanState getState() {
        return state;
    }

    public void setState(LoanState state) {
        this.state = state;
    }
}
