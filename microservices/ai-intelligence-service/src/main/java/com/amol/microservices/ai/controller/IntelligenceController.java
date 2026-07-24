package com.amol.microservices.ai.controller;

import com.amol.microservices.ai.model.AssistantRequest;
import com.amol.microservices.ai.model.ChurnRequest;
import com.amol.microservices.ai.model.CreditRequest;
import com.amol.microservices.ai.model.FraudRequest;
import com.amol.microservices.ai.model.SegmentRequest;
import com.amol.microservices.ai.service.AssistantService;
import com.amol.microservices.ai.service.ChurnService;
import com.amol.microservices.ai.service.CreditScoringService;
import com.amol.microservices.ai.service.FraudDetectionService;
import com.amol.microservices.ai.service.SegmentationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** AI/ML inference API — fraud, credit, churn, segmentation, and the GenAI assistant. */
@RestController
@Tag(name = "AI/ML Intelligence", description = "Inference endpoints for the intelligence layer")
public class IntelligenceController {

    private final FraudDetectionService fraud;
    private final CreditScoringService credit;
    private final ChurnService churn;
    private final SegmentationService segmentation;
    private final AssistantService assistant;

    public IntelligenceController(FraudDetectionService fraud, CreditScoringService credit,
                                  ChurnService churn, SegmentationService segmentation,
                                  AssistantService assistant) {
        this.fraud = fraud;
        this.credit = credit;
        this.churn = churn;
        this.segmentation = segmentation;
        this.assistant = assistant;
    }

    @PostMapping("/fraud/score")
    @Operation(summary = "Score a transaction for fraud")
    public ResponseEntity<FraudDetectionService.Result> fraud(@Valid @RequestBody FraudRequest request) {
        return ResponseEntity.ok(fraud.score(request));
    }

    @PostMapping("/credit/score")
    @Operation(summary = "Compute a credit score")
    public ResponseEntity<CreditScoringService.Result> credit(@Valid @RequestBody CreditRequest request) {
        return ResponseEntity.ok(credit.score(request));
    }

    @PostMapping("/churn/predict")
    @Operation(summary = "Predict churn probability")
    public ResponseEntity<ChurnService.Result> churn(@Valid @RequestBody ChurnRequest request) {
        return ResponseEntity.ok(churn.predict(request));
    }

    @PostMapping("/segment")
    @Operation(summary = "Segment a customer and recommend a next-best-offer")
    public ResponseEntity<SegmentationService.Result> segment(@Valid @RequestBody SegmentRequest request) {
        return ResponseEntity.ok(segmentation.segment(request));
    }

    @PostMapping("/assistant/ask")
    @Operation(summary = "Ask the GenAI assistant")
    public ResponseEntity<AssistantService.Result> ask(@Valid @RequestBody AssistantRequest request) {
        return ResponseEntity.ok(assistant.ask(request));
    }
}
