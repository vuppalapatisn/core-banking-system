package com.amol.microservices.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI / OpenAPI metadata for the AI/ML intelligence service. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI/ML & Intelligence Service")
                        .description("Fraud detection, credit scoring, AML screening, segmentation, "
                                + "churn prediction, and a GenAI assistant.")
                        .version("0.1.0"));
    }
}
