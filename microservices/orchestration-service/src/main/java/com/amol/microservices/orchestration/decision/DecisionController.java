package com.amol.microservices.orchestration.decision;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Decision Orchestration API — combine multiple rule sets into a single decision. */
@RestController
@RequestMapping("/decisions")
@Tag(name = "Decision Orchestration", description = "Coordinate decisioning across multiple rule sets")
public class DecisionController {

    private static final List<String> DEFAULT_RULE_SETS = List.of("credit", "risk");

    private final DecisionOrchestrator orchestrator;

    public DecisionController(DecisionOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    @Operation(summary = "Orchestrate a decision",
            description = "Runs the given rule sets (default: credit + risk) and combines them by "
                    + "severity — any REJECT wins, else any REVIEW, else APPROVE.")
    public ResponseEntity<DecisionResult> decide(@Valid @RequestBody DecisionRequest request) {
        List<String> ruleSets = (request.ruleSets() == null || request.ruleSets().isEmpty())
                ? DEFAULT_RULE_SETS
                : request.ruleSets();
        return ResponseEntity.ok(orchestrator.decide(ruleSets, request.facts()));
    }
}
