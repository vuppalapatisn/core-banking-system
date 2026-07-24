package com.amol.microservices.ai.service;

import com.amol.microservices.ai.model.FraudRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Fraud detection. Combines weighted risk signals (large amount, cross-border, wire rail,
 * high velocity) into a 0..1 score and an ALLOW / REVIEW / BLOCK decision, with human-readable
 * reasons. A reference heuristic model; a trained classifier swaps in behind this method.
 */
@Service
public class FraudDetectionService {

    private static final long LARGE_AMOUNT_MINOR = 500_000; // 5,000.00
    private static final int HIGH_VELOCITY = 10;

    public record Result(double score, String decision, List<String> reasons) {
    }

    private final MeterRegistry meterRegistry;

    public FraudDetectionService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Result score(FraudRequest r) {
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        if (r.amountMinor() > LARGE_AMOUNT_MINOR) {
            score += 0.30;
            reasons.add("large amount");
        }
        if (r.homeCountry() != null && r.country() != null && !r.homeCountry().equalsIgnoreCase(r.country())) {
            score += 0.30;
            reasons.add("cross-border transaction");
        }
        if ("WIRE".equalsIgnoreCase(r.network())) {
            score += 0.20;
            reasons.add("wire rail");
        }
        if (r.recentTxnCount() > HIGH_VELOCITY) {
            score += 0.20;
            reasons.add("high transaction velocity");
        }
        score = Math.min(1.0, score);

        String decision = score >= 0.7 ? "BLOCK" : score >= 0.4 ? "REVIEW" : "ALLOW";
        Counter.builder("ai.fraud.decisions").tag("decision", decision).register(meterRegistry).increment();
        return new Result(score, decision, reasons);
    }
}
