package com.amol.microservices.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI/ML &amp; Intelligence layer — inference services for fraud detection, credit scoring, AML
 * screening, customer segmentation / next-best-offer, churn prediction, and a GenAI assistant.
 *
 * <p>Models are lightweight, deterministic reference implementations so the service is self-contained
 * and testable; each sits behind a seam a real ML model / LLM swaps into for production.
 */
@SpringBootApplication
public class AiIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiIntelligenceApplication.class, args);
    }
}
