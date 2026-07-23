package com.amol.microservices.orchestration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI / OpenAPI metadata for the orchestration &amp; integration service. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orchestrationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Orchestration & Integration Service")
                        .description("BPM workflow engine, externalized business rules engine, decision "
                                + "orchestration, and ESB-style content-based routing with resilience.")
                        .version("0.1.0"));
    }
}
