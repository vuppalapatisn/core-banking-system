package com.amol.microservices.orchestration.rules;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RulesEngineTest {

    private RulesEngine engine;

    @BeforeEach
    void setUp() {
        OrchestrationProperties props = new OrchestrationProperties();
        props.getRules().put("credit", List.of(
                new RuleDefinition("reject-low", "creditScore", Operator.LT, 600, Outcome.REJECT, "low score"),
                new RuleDefinition("approve-default", "creditScore", Operator.GTE, 600, Outcome.APPROVE, "ok")));
        engine = new RulesEngine(props);
    }

    @Test
    void firesRejectForLowScore() {
        List<RuleMatch> matches = engine.evaluate("credit", Map.of("creditScore", 550.0));
        assertThat(matches).extracting(RuleMatch::outcome).containsExactly(Outcome.REJECT);
    }

    @Test
    void firesApproveForGoodScore() {
        List<RuleMatch> matches = engine.evaluate("credit", Map.of("creditScore", 720.0));
        assertThat(matches).extracting(RuleMatch::ruleName).containsExactly("approve-default");
    }

    @Test
    void noMatchWhenFactAbsent() {
        assertThat(engine.evaluate("credit", Map.of("other", 1.0))).isEmpty();
    }

    @Test
    void unknownRuleSetIsRejected() {
        assertThatThrownBy(() -> engine.evaluate("nope", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
