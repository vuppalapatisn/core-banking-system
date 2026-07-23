package com.amol.microservices.orchestration.workflow;

/** States of the loan-approval business process. */
public enum WorkflowState {
    SUBMITTED,
    APPROVED,
    DISBURSED,
    MANUAL_REVIEW,
    REJECTED
}
