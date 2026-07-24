package com.amol.microservices.ai.model;

/** Engagement features for predicting churn. */
public record ChurnRequest(
        int tenureMonths,
        int monthlyTxns,
        int supportTickets,
        int productCount,
        int daysSinceLastLogin) {
}
