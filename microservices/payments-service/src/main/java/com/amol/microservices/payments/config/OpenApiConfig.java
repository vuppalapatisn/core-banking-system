package com.amol.microservices.payments.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI / OpenAPI metadata for the payments service. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payments Switch / Gateway")
                        .description("Payment submission, network routing, and idempotent processing.")
                        .version("0.1.0"));
    }
}
