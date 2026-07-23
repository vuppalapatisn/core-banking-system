package com.amol.microservices.gateway.security;

import java.util.List;

/**
 * An IAM client identity: its id, a BCrypt-hashed secret, the roles it is granted (RBAC),
 * and whether a second factor (MFA/OTP) is required to obtain a token.
 *
 * <p>The plaintext secret is never stored — only {@code secretHash}.
 */
public record ClientAccount(
        String clientId,
        String secretHash,
        List<String> roles,
        boolean mfaEnabled) {
}
