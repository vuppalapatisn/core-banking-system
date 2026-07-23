package com.amol.microservices.cbs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI / OpenAPI metadata for the core banking service. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cbsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Core Banking System (CBS)")
                        .description("Customers, CASA accounts, and double-entry general-ledger postings.")
                        .version("0.1.0"));
    }
}
