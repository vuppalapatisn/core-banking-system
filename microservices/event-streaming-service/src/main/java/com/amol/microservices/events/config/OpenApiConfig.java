package com.amol.microservices.events.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI / OpenAPI metadata for the event streaming service. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event Streaming Service")
                        .description("Kafka-style topics, partitions, offset logs, consumer groups, and CDC.")
                        .version("0.1.0"));
    }
}
