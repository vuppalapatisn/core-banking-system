package com.amol.microservices.orchestration.rules;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Business Rules Engine. Evaluates a named, externalized rule set (loaded from configuration) against
 * a set of numeric facts and returns every rule that fired. Rules are data, not code — changing
 * {@code orchestration.rules.*} changes policy without a redeploy.
 */
@Service
public class RulesEngine {

    private final OrchestrationProperties properties;

    public RulesEngine(OrchestrationProperties properties) {
        this.properties = properties;
    }

    /**
     * Evaluates the rule set {@code ruleSet} against {@code facts}.
     *
     * @throws IllegalArgumentException if the rule set is not configured
     */
    public List<RuleMatch> evaluate(String ruleSet, Map<String, Double> facts) {
        List<RuleDefinition> rules = properties.getRules().get(ruleSet);
        if (rules == null) {
            throw new IllegalArgumentException("Unknown rule set: " + ruleSet);
        }
        List<RuleMatch> matches = new ArrayList<>();
        for (RuleDefinition rule : rules) {
            Double factValue = facts == null ? null : facts.get(rule.getAttribute());
            if (factValue != null && rule.getOperator().test(factValue, rule.getValue())) {
                matches.add(new RuleMatch(rule.getName(), rule.getAttribute(), rule.getOutcome(), rule.getMessage()));
            }
        }
        return matches;
    }

    /** Names of all configured rule sets. */
    public List<String> ruleSets() {
        return properties.ruleSetNames();
    }

    /** Rules in a given set (for the listing endpoint); empty if the set is unknown. */
    public List<RuleDefinition> rulesIn(String ruleSet) {
        return properties.getRules().getOrDefault(ruleSet, List.of());
    }
}
