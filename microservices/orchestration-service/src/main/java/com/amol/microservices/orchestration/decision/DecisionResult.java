package com.amol.microservices.orchestration.decision;

import com.amol.microservices.orchestration.rules.Outcome;
import com.amol.microservices.orchestration.rules.RuleMatch;

import java.util.List;

/** The orchestrated decision plus the rule sets consulted and the matches that justified it. */
public record DecisionResult(
        Outcome decision,
        List<String> ruleSetsEvaluated,
        List<RuleMatch> reasons) {
}
