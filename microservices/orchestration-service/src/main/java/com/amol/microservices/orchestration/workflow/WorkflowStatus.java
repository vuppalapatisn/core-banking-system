package com.amol.microservices.orchestration.workflow;

/** Lifecycle status of a workflow instance. RUNNING instances can be advanced; the others are terminal. */
public enum WorkflowStatus {
    RUNNING,
    PENDING_REVIEW,
    COMPLETED,
    REJECTED;

    public boolean isTerminal() {
        return this != RUNNING;
    }
}
