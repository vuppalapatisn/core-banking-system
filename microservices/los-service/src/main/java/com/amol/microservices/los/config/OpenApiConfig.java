package com.amol.microservices.los.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI / OpenAPI metadata for the loan origination service. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI losOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loan Origination System (LOS)")
                        .description("Loan application intake, underwriting, and origination.")
                        .version("0.1.0"));
    }
}
