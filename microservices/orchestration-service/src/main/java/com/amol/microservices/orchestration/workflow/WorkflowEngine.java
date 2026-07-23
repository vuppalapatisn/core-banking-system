package com.amol.microservices.orchestration.workflow;

import com.amol.microservices.orchestration.decision.DecisionOrchestrator;
import com.amol.microservices.orchestration.decision.DecisionResult;
import com.amol.microservices.orchestration.integration.CanonicalMessage;
import com.amol.microservices.orchestration.integration.MessageRouter;
import com.amol.microservices.orchestration.integration.RoutingResult;
import com.amol.microservices.orchestration.rules.Outcome;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BPM / Workflow engine. Drives the multi-step loan-approval business process as a state machine,
 * delegating decisioning to the {@link DecisionOrchestrator} and downstream hand-off (disbursement)
 * to the {@link MessageRouter}. Instances are held in memory (a durable store would be the
 * productionization step; the state model would not change).
 */
@Service
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);
    private static final String LOAN_APPROVAL = "loan-approval";
    private static final List<String> DECISION_RULE_SETS = List.of("credit", "risk");

    private final DecisionOrchestrator decisionOrchestrator;
    private final MessageRouter messageRouter;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, WorkflowInstance> instances = new ConcurrentHashMap<>();

    public WorkflowEngine(DecisionOrchestrator decisionOrchestrator, MessageRouter messageRouter,
                          MeterRegistry meterRegistry) {
        this.decisionOrchestrator = decisionOrchestrator;
        this.messageRouter = messageRouter;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Starts a workflow instance.
     *
     * @throws IllegalArgumentException if the workflow type is unsupported
     */
    public WorkflowInstance start(String type, Map<String, Double> facts) {
        if (!LOAN_APPROVAL.equals(type)) {
            throw new IllegalArgumentException("Unsupported workflow type: " + type);
        }
        String id = UUID.randomUUID().toString();
        WorkflowInstance instance = new WorkflowInstance(
                id, type, facts == null ? Map.of() : facts, WorkflowState.SUBMITTED, WorkflowStatus.RUNNING);
        instance.recordTransition("submitted");
        instances.put(id, instance);
        counter("started").increment();
        log.info("workflow_started id={} type={}", id, type);
        return instance;
    }

    /** Fetches an instance or fails if unknown. */
    public WorkflowInstance get(String id) {
        WorkflowInstance instance = instances.get(id);
        if (instance == null) {
            throw new IllegalArgumentException("Unknown workflow instance: " + id);
        }
        return instance;
    }

    /**
     * Advances an instance one step through the state machine.
     *
     * @throws IllegalStateException if the instance is already in a terminal status
     */
    public WorkflowInstance advance(String id) {
        WorkflowInstance instance = get(id);
        if (instance.getStatus().isTerminal()) {
            throw new IllegalStateException("Workflow " + id + " is not running (status="
                    + instance.getStatus() + ")");
        }

        switch (instance.getState()) {
            case SUBMITTED -> runDecision(instance);
            case APPROVED -> runDisbursement(instance);
            default -> throw new IllegalStateException("No transition from state " + instance.getState());
        }
        return instance;
    }

    private void runDecision(WorkflowInstance instance) {
        DecisionResult result = decisionOrchestrator.decide(DECISION_RULE_SETS, instance.getFacts());
        instance.setDecision(result);
        Outcome outcome = result.decision();
        switch (outcome) {
            case REJECT -> {
                instance.setState(WorkflowState.REJECTED);
                instance.setStatus(WorkflowStatus.REJECTED);
            }
            case REVIEW -> {
                instance.setState(WorkflowState.MANUAL_REVIEW);
                instance.setStatus(WorkflowStatus.PENDING_REVIEW);
            }
            case APPROVE -> {
                instance.setState(WorkflowState.APPROVED);
                instance.setStatus(WorkflowStatus.RUNNING);
            }
        }
        instance.recordTransition("decision=" + outcome);
        log.info("workflow_decision id={} outcome={}", instance.getId(), outcome);
    }

    private void runDisbursement(WorkflowInstance instance) {
        CanonicalMessage message = new CanonicalMessage(
                "loan", "disbursement", Map.copyOf(instance.getFacts()));
        RoutingResult routing = messageRouter.route(message);
        instance.setRoutedTo(routing.destination());
        instance.setState(WorkflowState.DISBURSED);
        instance.setStatus(WorkflowStatus.COMPLETED);
        instance.recordTransition("disbursed->" + routing.destination());
        counter("completed").increment();
        log.info("workflow_completed id={} routedTo={}", instance.getId(), routing.destination());
    }

    private Counter counter(String event) {
        return Counter.builder("orchestration.workflows")
                .tag("event", event)
                .description("Workflow lifecycle events")
                .register(meterRegistry);
    }
}
