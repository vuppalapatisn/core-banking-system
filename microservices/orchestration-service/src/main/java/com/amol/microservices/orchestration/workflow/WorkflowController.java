package com.amol.microservices.orchestration.workflow;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BPM / Workflow API — start, advance, and inspect business-process instances. */
@RestController
@RequestMapping("/workflows")
@Tag(name = "BPM / Workflow", description = "Multi-step business process orchestration (loan approval)")
public class WorkflowController {

    private final WorkflowEngine engine;

    public WorkflowController(WorkflowEngine engine) {
        this.engine = engine;
    }

    @PostMapping
    @Operation(summary = "Start a workflow instance",
            description = "Creates a new instance (e.g. type 'loan-approval') in the SUBMITTED state.")
    public ResponseEntity<WorkflowInstance> start(@Valid @RequestBody StartWorkflowRequest request) {
        return ResponseEntity.ok(engine.start(request.type(), request.facts()));
    }

    @PostMapping("/{id}/advance")
    @Operation(summary = "Advance a workflow instance one step",
            description = "Runs the next step of the state machine (decisioning, then disbursement).")
    public ResponseEntity<WorkflowInstance> advance(@PathVariable String id) {
        return ResponseEntity.ok(engine.advance(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a workflow instance")
    public ResponseEntity<WorkflowInstance> get(@PathVariable String id) {
        return ResponseEntity.ok(engine.get(id));
    }
}
