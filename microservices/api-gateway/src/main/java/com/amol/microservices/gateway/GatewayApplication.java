package com.amol.microservices.gateway;

import com.amol.microservices.gateway.config.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * API Management &amp; Security layer for the platform.
 *
 * <p>Fronts the backend microservices and provides OAuth2/JWT authentication, MFA,
 * a Web Application Firewall, rate limiting, RBAC authorization, and tokenization —
 * the components of the "API Management &amp; Security Layer" in the target architecture.
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
