package com.amol.microservices.gateway.service;

import com.amol.microservices.gateway.exception.InvalidCredentialsException;
import com.amol.microservices.gateway.exception.MfaRequiredException;
import com.amol.microservices.gateway.model.TokenRequest;
import com.amol.microservices.gateway.model.TokenResponse;
import com.amol.microservices.gateway.security.ClientAccount;
import com.amol.microservices.gateway.security.IamClientRegistry;
import com.amol.microservices.gateway.security.JwtService;
import com.amol.microservices.gateway.security.OtpService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the OAuth2-style token grant: verify client credentials (IAM), enforce the MFA
 * second factor when the client requires it, then mint a JWT carrying the client's RBAC roles.
 *
 * <p>Never logs credentials or OTP values — only the client id, outcome, and a safe reason.
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final IamClientRegistry registry;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final Counter tokenIssued;
    private final Counter tokenDenied;

    public AuthenticationService(
            IamClientRegistry registry,
            OtpService otpService,
            JwtService jwtService,
            MeterRegistry meterRegistry) {
        this.registry = registry;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.tokenIssued = Counter.builder("gateway.auth.tokens")
                .tag("outcome", "issued")
                .description("Access tokens issued by the gateway")
                .register(meterRegistry);
        this.tokenDenied = Counter.builder("gateway.auth.tokens")
                .tag("outcome", "denied")
                .description("Token requests denied by the gateway")
                .register(meterRegistry);
    }

    public TokenResponse authenticate(TokenRequest request) {
        ClientAccount account = registry.find(request.clientId())
                .filter(a -> registry.secretMatches(a, request.clientSecret()))
                .orElseThrow(() -> {
                    tokenDenied.increment();
                    log.warn("auth_failed clientId={} reason=invalid_credentials", request.clientId());
                    return new InvalidCredentialsException("Invalid client credentials");
                });

        if (account.mfaEnabled() && !otpService.verify(request.otp())) {
            tokenDenied.increment();
            log.warn("auth_failed clientId={} reason=mfa_required", account.clientId());
            throw new MfaRequiredException("A valid MFA one-time code is required for this client");
        }

        TokenResponse token = jwtService.issue(account.clientId(), account.roles());
        tokenIssued.increment();
        log.info("auth_succeeded clientId={} roles={}", account.clientId(), account.roles());
        return token;
    }
}
