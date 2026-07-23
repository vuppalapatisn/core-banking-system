package com.amol.microservices.orchestration.rules;

/** Comparison operators supported by the externalized business rules. */
public enum Operator {
    LT, LTE, GT, GTE, EQ, NEQ;

    /** Evaluates {@code factValue <op> ruleValue}. */
    public boolean test(double factValue, double ruleValue) {
        return switch (this) {
            case LT -> factValue < ruleValue;
            case LTE -> factValue <= ruleValue;
            case GT -> factValue > ruleValue;
            case GTE -> factValue >= ruleValue;
            case EQ -> factValue == ruleValue;
            case NEQ -> factValue != ruleValue;
        };
    }
}
