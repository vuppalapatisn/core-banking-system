package com.amol.microservices.orchestration.journey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** End-to-end orchestration API — runs a loan across LOS, LMS, and Payments in one call. */
@RestController
@RequestMapping("/journeys")
@Tag(name = "Loan Journey", description = "End-to-end orchestration: LOS → LMS → Payments")
public class LoanJourneyController {

    private final LoanJourneyService service;

    public LoanJourneyController(LoanJourneyService service) {
        this.service = service;
    }

    @PostMapping("/loan")
    @Operation(summary = "Run the end-to-end loan journey",
            description = "Originates the loan (LOS), and if approved books it (LMS) and disburses it "
                    + "(Payments), propagating the correlation id across every hop.")
    public ResponseEntity<LoanJourneyResult> loan(@Valid @RequestBody JourneyRequest request) {
        return ResponseEntity.ok(service.run(request));
    }
}
