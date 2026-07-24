package com.amol.microservices.ai.service;

import com.amol.microservices.ai.model.CreditRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Credit scoring. Produces a 300..850 score from income, debt burden, delinquencies, and
 * utilization, plus a band and the factors that moved it. Reference scorecard; a trained model
 * (or bureau score) swaps in behind this method.
 */
@Service
public class CreditScoringService {

    private static final int MIN = 300;
    private static final int MAX = 850;

    public record Result(int score, String band, List<String> factors) {
    }

    public Result score(CreditRequest r) {
        List<String> factors = new ArrayList<>();
        double score = 650;

        score -= 40.0 * Math.max(0, r.delinquencies());
        if (r.delinquencies() > 0) {
            factors.add(r.delinquencies() + " delinquency(ies)");
        }

        double utilization = Math.max(0, r.creditUtilizationPct());
        if (utilization > 30) {
            score -= (utilization - 30) * 2.0;
            factors.add("high credit utilization");
        } else {
            score += 20;
            factors.add("low credit utilization");
        }

        // Debt-to-income (monthly): annualIncome/12 vs monthly debt.
        double monthlyIncome = r.annualIncomeMinor() / 12.0;
        double dti = monthlyIncome > 0 ? r.monthlyDebtMinor() / monthlyIncome : 1.0;
        if (dti > 0.4) {
            score -= (dti - 0.4) * 200;
            factors.add("high debt-to-income");
        } else {
            score += 30;
            factors.add("healthy debt-to-income");
        }

        if (r.ageYears() >= 25) {
            score += 15;
        }

        int clamped = (int) Math.round(Math.max(MIN, Math.min(MAX, score)));
        return new Result(clamped, band(clamped), factors);
    }

    private String band(int score) {
        if (score >= 750) {
            return "EXCELLENT";
        }
        if (score >= 670) {
            return "GOOD";
        }
        if (score >= 580) {
            return "FAIR";
        }
        return "POOR";
    }
}
