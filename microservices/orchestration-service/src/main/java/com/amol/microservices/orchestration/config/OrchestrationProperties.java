package com.amol.microservices.orchestration.config;

import com.amol.microservices.orchestration.rules.RuleDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type-safe configuration for the Orchestration &amp; Integration layer, bound from {@code orchestration.*}.
 * Business rules and integration routes are externalized here so policy and routing can change
 * without a code deploy.
 */
@ConfigurationProperties(prefix = "orchestration")
public class OrchestrationProperties {

    /** Named rule sets (e.g. "credit", "risk") -> ordered rules. */
    private Map<String, List<RuleDefinition>> rules = new HashMap<>();

    @NestedConfigurationProperty
    private final Integration integration = new Integration();

    public Map<String, List<RuleDefinition>> getRules() {
        return rules;
    }

    public void setRules(Map<String, List<RuleDefinition>> rules) {
        this.rules = rules;
    }

    public Integration getIntegration() {
        return integration;
    }

    public static class Integration {
        /** Content-based routes: message domain -> (message type -> logical destination). */
        private Map<String, Map<String, String>> routes = new HashMap<>();

        @NestedConfigurationProperty
        private final Resilience resilience = new Resilience();

        public Map<String, Map<String, String>> getRoutes() {
            return routes;
        }

        public void setRoutes(Map<String, Map<String, String>> routes) {
            this.routes = routes;
        }

        public Resilience getResilience() {
            return resilience;
        }
    }

    public static class Resilience {
        private int maxRetries = 2;
        private int circuitFailureThreshold = 5;
        private long circuitOpenMillis = 10_000;

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getCircuitFailureThreshold() {
            return circuitFailureThreshold;
        }

        public void setCircuitFailureThreshold(int circuitFailureThreshold) {
            this.circuitFailureThreshold = circuitFailureThreshold;
        }

        public long getCircuitOpenMillis() {
            return circuitOpenMillis;
        }

        public void setCircuitOpenMillis(long circuitOpenMillis) {
            this.circuitOpenMillis = circuitOpenMillis;
        }
    }

    /** All configured rule-set names (for diagnostics / the rules listing endpoint). */
    public List<String> ruleSetNames() {
        return new ArrayList<>(rules.keySet());
    }
}
