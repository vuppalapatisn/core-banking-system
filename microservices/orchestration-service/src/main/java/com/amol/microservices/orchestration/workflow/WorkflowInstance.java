package com.amol.microservices.orchestration.workflow;

import com.amol.microservices.orchestration.decision.DecisionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A running (or completed) instance of a business-process workflow. */
public class WorkflowInstance {

    private final String id;
    private final String type;
    private final Map<String, Double> facts;
    private final List<String> history = new ArrayList<>();

    private WorkflowState state;
    private WorkflowStatus status;
    private DecisionResult decision;
    private String routedTo;

    public WorkflowInstance(String id, String type, Map<String, Double> facts,
                            WorkflowState state, WorkflowStatus status) {
        this.id = id;
        this.type = type;
        this.facts = facts;
        this.state = state;
        this.status = status;
    }

    public void recordTransition(String event) {
        history.add(event);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Map<String, Double> getFacts() {
        return facts;
    }

    public List<String> getHistory() {
        return history;
    }

    public WorkflowState getState() {
        return state;
    }

    public void setState(WorkflowState state) {
        this.state = state;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public DecisionResult getDecision() {
        return decision;
    }

    public void setDecision(DecisionResult decision) {
        this.decision = decision;
    }

    public String getRoutedTo() {
        return routedTo;
    }

    public void setRoutedTo(String routedTo) {
        this.routedTo = routedTo;
    }
}
