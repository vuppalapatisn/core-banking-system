package com.amol.microservices.orchestration.rules;

/**
 * Possible outcomes a rule (or an orchestrated decision) can produce.
 * Severity order for combining decisions: REJECT &gt; REVIEW &gt; APPROVE.
 */
public enum Outcome {
    APPROVE(0),
    REVIEW(1),
    REJECT(2);

    private final int severity;

    Outcome(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }
}
