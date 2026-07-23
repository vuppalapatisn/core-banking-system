package com.amol.microservices.orchestration.rules;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Business Rules Engine API — evaluate externalized rule sets and inspect configured rules. */
@RestController
@RequestMapping("/rules")
@Tag(name = "Business Rules Engine", description = "Evaluate externalized, config-driven decision rules")
public class RulesController {

    private final RulesEngine rulesEngine;

    public RulesController(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate a rule set against facts",
            description = "Returns every rule in the set that fired for the supplied facts.")
    public ResponseEntity<Map<String, Object>> evaluate(@Valid @RequestBody EvaluateRequest request) {
        List<RuleMatch> matches = rulesEngine.evaluate(request.ruleSet(), request.facts());
        return ResponseEntity.ok(Map.of(
                "ruleSet", request.ruleSet(),
                "matched", matches.size(),
                "matches", matches));
    }

    @GetMapping
    @Operation(summary = "List configured rule sets")
    public ResponseEntity<Map<String, Object>> ruleSets() {
        List<String> sets = rulesEngine.ruleSets();
        return ResponseEntity.ok(Map.of("ruleSets", sets));
    }

    @GetMapping("/{ruleSet}")
    @Operation(summary = "List the rules in a rule set")
    public ResponseEntity<List<RuleDefinition>> rulesIn(
            @org.springframework.web.bind.annotation.PathVariable String ruleSet) {
        return ResponseEntity.ok(rulesEngine.rulesIn(ruleSet));
    }
}
