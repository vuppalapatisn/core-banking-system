package com.amol.microservices.orchestration.decision;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import com.amol.microservices.orchestration.rules.Operator;
import com.amol.microservices.orchestration.rules.Outcome;
import com.amol.microservices.orchestration.rules.RuleDefinition;
import com.amol.microservices.orchestration.rules.RulesEngine;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecisionOrchestratorTest {

    private DecisionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        OrchestrationProperties props = new OrchestrationProperties();
        props.getRules().put("credit", List.of(
                new RuleDefinition("reject-low", "creditScore", Operator.LT, 600, Outcome.REJECT, "low"),
                new RuleDefinition("review-amount", "amount", Operator.GT, 100000, Outcome.REVIEW, "big"),
                new RuleDefinition("approve", "creditScore", Operator.GTE, 600, Outcome.APPROVE, "ok")));
        props.getRules().put("risk", List.of(
                new RuleDefinition("reject-dti", "debtToIncome", Operator.GT, 0.45, Outcome.REJECT, "dti")));
        orchestrator = new DecisionOrchestrator(new RulesEngine(props), new SimpleMeterRegistry());
    }

    @Test
    void approvesWhenAllGood() {
        DecisionResult r = orchestrator.decide(List.of("credit", "risk"),
                Map.of("creditScore", 720.0, "amount", 5000.0, "debtToIncome", 0.2));
        assertThat(r.decision()).isEqualTo(Outcome.APPROVE);
    }

    @Test
    void rejectWinsOverReview() {
        // amount triggers REVIEW (credit) but low score triggers REJECT — REJECT must win.
        DecisionResult r = orchestrator.decide(List.of("credit", "risk"),
                Map.of("creditScore", 500.0, "amount", 200000.0, "debtToIncome", 0.2));
        assertThat(r.decision()).isEqualTo(Outcome.REJECT);
    }

    @Test
    void reviewWhenOnlyReviewFires() {
        DecisionResult r = orchestrator.decide(List.of("credit", "risk"),
                Map.of("creditScore", 700.0, "amount", 200000.0, "debtToIncome", 0.2));
        assertThat(r.decision()).isEqualTo(Outcome.REVIEW);
    }

    @Test
    void emptyRuleSetsRejected() {
        assertThatThrownBy(() -> orchestrator.decide(List.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
