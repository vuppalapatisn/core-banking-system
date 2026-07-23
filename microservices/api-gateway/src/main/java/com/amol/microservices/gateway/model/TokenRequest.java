package com.amol.microservices.gateway.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Credentials for the OAuth2-style token endpoint. {@code otp} is the second factor and is
 * required only for MFA-enabled principals (enforced server-side).
 */
public record TokenRequest(
        @NotBlank(message = "clientId is required") String clientId,
        @NotBlank(message = "clientSecret is required") String clientSecret,
        String otp) {
}
