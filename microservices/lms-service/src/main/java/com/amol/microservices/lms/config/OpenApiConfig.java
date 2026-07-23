package com.amol.microservices.lms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI / OpenAPI metadata for the loan management service. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Loan Management System (LMS)")
                        .description("Loan servicing: amortization schedules, repayments, and payoff.")
                        .version("0.1.0"));
    }
}
