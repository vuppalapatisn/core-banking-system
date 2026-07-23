package com.amol.microservices.orchestration.decision;

import com.amol.microservices.orchestration.rules.Outcome;
import com.amol.microservices.orchestration.rules.RuleMatch;
import com.amol.microservices.orchestration.rules.RulesEngine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Decision Orchestration. Coordinates automated decisioning across multiple rule sets ("models"):
 * it runs each configured rule set through the {@link RulesEngine}, then combines the results by
 * severity — any REJECT wins, else any REVIEW wins, else APPROVE.
 */
@Service
public class DecisionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DecisionOrchestrator.class);

    private final RulesEngine rulesEngine;
    private final MeterRegistry meterRegistry;

    public DecisionOrchestrator(RulesEngine rulesEngine, MeterRegistry meterRegistry) {
        this.rulesEngine = rulesEngine;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Orchestrates a decision over {@code facts} using the given {@code ruleSets}.
     *
     * @throws IllegalArgumentException if any rule set is unknown
     */
    public DecisionResult decide(List<String> ruleSets, Map<String, Double> facts) {
        if (ruleSets == null || ruleSets.isEmpty()) {
            throw new IllegalArgumentException("At least one rule set is required");
        }
        List<RuleMatch> reasons = new ArrayList<>();
        Outcome decision = Outcome.APPROVE;
        for (String ruleSet : ruleSets) {
            for (RuleMatch match : rulesEngine.evaluate(ruleSet, facts)) {
                reasons.add(match);
                if (match.outcome().severity() > decision.severity()) {
                    decision = match.outcome();
                }
            }
        }
        Counter.builder("orchestration.decisions")
                .tag("outcome", decision.name())
                .description("Decisions produced by the decision orchestrator")
                .register(meterRegistry)
                .increment();
        log.info("decision_orchestrated ruleSets={} outcome={} reasons={}",
                ruleSets, decision, reasons.size());
        return new DecisionResult(decision, ruleSets, reasons);
    }
}
