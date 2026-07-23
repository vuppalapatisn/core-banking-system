package com.amol.microservices.orchestration.rules;

/**
 * A single externalized business rule: when {@code attribute} compared to {@code value}
 * via {@code operator} holds, the rule fires and contributes {@code outcome} (with {@code message}).
 *
 * <p>Mutable bean so it can be bound from configuration ({@code orchestration.rules.*}); this is
 * what lets credit/risk policy change without a code deploy.
 */
public class RuleDefinition {

    private String name;
    private String attribute;
    private Operator operator;
    private double value;
    private Outcome outcome;
    private String message;

    public RuleDefinition() {
    }

    public RuleDefinition(String name, String attribute, Operator operator, double value,
                          Outcome outcome, String message) {
        this.name = name;
        this.attribute = attribute;
        this.operator = operator;
        this.value = value;
        this.outcome = outcome;
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
