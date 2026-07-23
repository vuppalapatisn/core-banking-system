package com.amol.microservices.gateway.model;

import java.util.List;

/** Issued access token plus its type, lifetime, and the granted roles (RBAC scopes). */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        List<String> roles) {
}
