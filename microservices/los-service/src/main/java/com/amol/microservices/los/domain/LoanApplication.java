package com.amol.microservices.los.domain;

/** A loan application moving through the origination lifecycle. */
public class LoanApplication {

    private final String id;
    private final String applicantId;
    private final long amountMinor;
    private final int termMonths;
    private final int creditScore;
    private LoanStatus status;
    private String decisionReason;

    public LoanApplication(String id, String applicantId, long amountMinor, int termMonths, int creditScore) {
        this.id = id;
        this.applicantId = applicantId;
        this.amountMinor = amountMinor;
        this.termMonths = termMonths;
        this.creditScore = creditScore;
        this.status = LoanStatus.RECEIVED;
    }

    public String getId() {
        return id;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }
}
