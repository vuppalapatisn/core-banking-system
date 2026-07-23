package com.amol.microservices.orchestration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrchestrationApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full context — rules engine, decision orchestrator, workflow engine,
        // ESB router, and resilient dispatcher — wires up from configuration.
    }
}
