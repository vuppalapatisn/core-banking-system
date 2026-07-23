package com.amol.microservices.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI / OpenAPI metadata, including the bearer-JWT scheme so the "Authorize" button in
 * Swagger UI lets you paste a token obtained from {@code POST /auth/token}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI apiGatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Management & Security Gateway")
                        .description("Front door for the platform: OAuth2/JWT authentication, MFA, WAF, "
                                + "rate limiting, RBAC, tokenization, and reverse-proxy to backend services.")
                        .version("0.1.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
