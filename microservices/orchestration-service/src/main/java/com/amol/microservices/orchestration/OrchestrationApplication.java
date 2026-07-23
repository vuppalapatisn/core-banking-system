package com.amol.microservices.orchestration;

import com.amol.microservices.orchestration.config.OrchestrationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Orchestration &amp; Integration layer for the platform.
 *
 * <p>Coordinates how systems talk and how business processes flow: a BPM workflow engine,
 * an externalized business rules engine, a decision orchestrator that combines rule sets,
 * and an ESB-style content-based message router with service-mesh-style resilience.
 */
@SpringBootApplication
@EnableConfigurationProperties(OrchestrationProperties.class)
public class OrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestrationApplication.class, args);
    }
}
