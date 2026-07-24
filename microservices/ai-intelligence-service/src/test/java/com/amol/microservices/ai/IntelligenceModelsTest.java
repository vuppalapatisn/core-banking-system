package com.amol.microservices.ai;

import com.amol.microservices.ai.model.ChurnRequest;
import com.amol.microservices.ai.model.CreditRequest;
import com.amol.microservices.ai.model.FraudRequest;
import com.amol.microservices.ai.model.SegmentRequest;
import com.amol.microservices.ai.service.AssistantService;
import com.amol.microservices.ai.service.ChurnService;
import com.amol.microservices.ai.service.CreditScoringService;
import com.amol.microservices.ai.service.DeterministicAssistantModel;
import com.amol.microservices.ai.service.FraudDetectionService;
import com.amol.microservices.ai.service.SegmentationService;
import com.amol.microservices.ai.model.AssistantRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntelligenceModelsTest {

    private final FraudDetectionService fraud = new FraudDetectionService(new SimpleMeterRegistry());
    private final CreditScoringService credit = new CreditScoringService();
    private final ChurnService churn = new ChurnService();
    private final SegmentationService segmentation = new SegmentationService();
    private final AssistantService assistant = new AssistantService(new DeterministicAssistantModel());

    @Test
    void fraudBlocksHighRiskAndAllowsBenign() {
        FraudDetectionService.Result risky = fraud.score(new FraudRequest(600_000, "WIRE", "US", "GB", 20));
        assertThat(risky.decision()).isEqualTo("BLOCK");
        assertThat(risky.score()).isGreaterThanOrEqualTo(0.7);

        FraudDetectionService.Result benign = fraud.score(new FraudRequest(1_000, "INTERNAL", "US", "US", 1));
        assertThat(benign.decision()).isEqualTo("ALLOW");
    }

    @Test
    void creditScoresGoodAndPoorProfiles() {
        CreditScoringService.Result good = credit.score(new CreditRequest(12_000_000, 100_000, 35, 0, 10));
        assertThat(good.score()).isGreaterThanOrEqualTo(670);

        CreditScoringService.Result poor = credit.score(new CreditRequest(3_600_000, 200_000, 30, 3, 80));
        assertThat(poor.score()).isLessThan(580);
        assertThat(poor.band()).isEqualTo("POOR");
    }

    @Test
    void churnHighForDisengagedLowForActive() {
        assertThat(churn.predict(new ChurnRequest(3, 0, 5, 1, 60)).risk()).isEqualTo("HIGH");
        assertThat(churn.predict(new ChurnRequest(24, 30, 0, 3, 1)).risk()).isEqualTo("LOW");
    }

    @Test
    void segmentationAndOffer() {
        SegmentationService.Result high = segmentation.segment(new SegmentRequest(10_000_000, 24, 1, 5));
        assertThat(high.segment()).isEqualTo("HIGH_VALUE");
        assertThat(high.nextBestOffer()).isNotBlank();

        assertThat(segmentation.segment(new SegmentRequest(1_000, 12, 1, 0)).segment()).isEqualTo("DORMANT");
    }

    @Test
    void assistantAnswersWithModelName() {
        AssistantService.Result r = assistant.ask(new AssistantRequest("How do I apply for a loan?", null));
        assertThat(r.answer().toLowerCase()).contains("loan");
        assertThat(r.model()).isEqualTo("deterministic-stub");
    }
}
