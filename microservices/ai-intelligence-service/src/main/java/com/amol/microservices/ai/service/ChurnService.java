package com.amol.microservices.ai.service;

import com.amol.microservices.ai.model.ChurnRequest;
import org.springframework.stereotype.Service;

/**
 * Churn prediction. Maps engagement features through a logistic function to a 0..1 probability and
 * a LOW / MEDIUM / HIGH risk band. Reference model; a trained classifier swaps in behind this method.
 */
@Service
public class ChurnService {

    public record Result(double probability, String risk) {
    }

    public Result predict(ChurnRequest r) {
        // Linear score: inactivity and support load push churn up; usage and tenure pull it down.
        double z = -1.0
                + 0.03 * Math.max(0, r.daysSinceLastLogin())
                + 0.20 * Math.max(0, r.supportTickets())
                - 0.05 * Math.max(0, r.monthlyTxns())
                - 0.02 * Math.max(0, r.tenureMonths())
                - 0.30 * Math.max(0, r.productCount());
        double probability = 1.0 / (1.0 + Math.exp(-z));
        double rounded = Math.round(probability * 1000.0) / 1000.0;
        String risk = rounded >= 0.66 ? "HIGH" : rounded >= 0.33 ? "MEDIUM" : "LOW";
        return new Result(rounded, risk);
    }
}
