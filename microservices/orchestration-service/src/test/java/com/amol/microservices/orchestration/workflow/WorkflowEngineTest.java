package com.amol.microservices.orchestration.workflow;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import com.amol.microservices.orchestration.decision.DecisionOrchestrator;
import com.amol.microservices.orchestration.integration.MessageRouter;
import com.amol.microservices.orchestration.integration.ResilientDispatcher;
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

class WorkflowEngineTest {

    private WorkflowEngine engine;

    @BeforeEach
    void setUp() {
        OrchestrationProperties props = new OrchestrationProperties();
        props.getRules().put("credit", List.of(
                new RuleDefinition("reject-low", "creditScore", Operator.LT, 600, Outcome.REJECT, "low"),
                new RuleDefinition("approve", "creditScore", Operator.GTE, 600, Outcome.APPROVE, "ok")));
        props.getRules().put("risk", List.of(
                new RuleDefinition("reject-dti", "debtToIncome", Operator.GT, 0.45, Outcome.REJECT, "dti")));
        props.getIntegration().setRoutes(Map.of("loan", Map.of("disbursement", "payments-switch")));

        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        RulesEngine rulesEngine = new RulesEngine(props);
        DecisionOrchestrator orchestrator = new DecisionOrchestrator(rulesEngine, reg);
        MessageRouter router = new MessageRouter(props, new ResilientDispatcher(props), reg);
        engine = new WorkflowEngine(orchestrator, router, reg);
    }

    @Test
    void happyPathReachesDisbursedCompleted() {
        WorkflowInstance i = engine.start("loan-approval",
                Map.of("creditScore", 720.0, "debtToIncome", 0.2));
        assertThat(i.getState()).isEqualTo(WorkflowState.SUBMITTED);

        engine.advance(i.getId()); // decisioning -> APPROVED
        assertThat(i.getState()).isEqualTo(WorkflowState.APPROVED);
        assertThat(i.getStatus()).isEqualTo(WorkflowStatus.RUNNING);

        engine.advance(i.getId()); // disbursement -> DISBURSED/COMPLETED
        assertThat(i.getState()).isEqualTo(WorkflowState.DISBURSED);
        assertThat(i.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(i.getRoutedTo()).isEqualTo("payments-switch");
    }

    @Test
    void lowScoreIsRejectedAndTerminal() {
        WorkflowInstance i = engine.start("loan-approval",
                Map.of("creditScore", 500.0, "debtToIncome", 0.2));
        engine.advance(i.getId());
        assertThat(i.getStatus()).isEqualTo(WorkflowStatus.REJECTED);
        assertThatThrownBy(() -> engine.advance(i.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unsupportedTypeRejected() {
        assertThatThrownBy(() -> engine.start("mortgage", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownInstanceRejected() {
        assertThatThrownBy(() -> engine.get("does-not-exist"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
