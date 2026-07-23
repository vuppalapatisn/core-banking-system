package com.amol.microservices.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory IAM registry of client identities and their RBAC roles — the identity source for
 * the token endpoint. Client secrets are supplied via environment variables (Kubernetes secrets
 * in a shared environment) and are only ever held as BCrypt hashes, never in plaintext.
 *
 * <p>This is a reference/demo registry seeded with two clients; a production deployment would
 * back this with an external IdP (OIDC provider) instead.
 */
@Component
public class IamClientRegistry {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, ClientAccount> clients;

    public IamClientRegistry(
            PasswordEncoder passwordEncoder,
            @Value("${gateway.security.clients.admin.secret:dev-admin-secret-change-me}") String adminSecret,
            @Value("${gateway.security.clients.service.secret:dev-service-secret-change-me}") String serviceSecret) {
        this.passwordEncoder = passwordEncoder;
        this.clients = Map.of(
                "gateway-admin", new ClientAccount(
                        "gateway-admin",
                        passwordEncoder.encode(adminSecret),
                        List.of("ADMIN", "USER"),
                        true),
                "gateway-service", new ClientAccount(
                        "gateway-service",
                        passwordEncoder.encode(serviceSecret),
                        List.of("USER"),
                        false));
    }

    public Optional<ClientAccount> find(String clientId) {
        if (clientId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(clients.get(clientId));
    }

    /** Constant-time-ish credential check via the {@link PasswordEncoder}. */
    public boolean secretMatches(ClientAccount account, String rawSecret) {
        return rawSecret != null && passwordEncoder.matches(rawSecret, account.secretHash());
    }
}
