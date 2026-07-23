package com.amol.microservices.gateway.security;

import com.amol.microservices.gateway.config.GatewayProperties;
import com.amol.microservices.gateway.exception.InvalidCredentialsException;
import com.amol.microservices.gateway.exception.MfaRequiredException;
import com.amol.microservices.gateway.model.TokenRequest;
import com.amol.microservices.gateway.model.TokenResponse;
import com.amol.microservices.gateway.service.AuthenticationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final String ADMIN_SECRET = "dev-admin-secret-change-me";
    private static final String SERVICE_SECRET = "dev-service-secret-change-me";
    private static final String TOTP_SEED = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

    private OtpService otpService;
    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        IamClientRegistry registry = new IamClientRegistry(encoder, ADMIN_SECRET, SERVICE_SECRET);
        otpService = new OtpService(TOTP_SEED);

        GatewayProperties props = new GatewayProperties();
        props.getSecurity().getJwt().setSecret("test-secret-that-is-at-least-32-bytes-long!!");
        JwtService jwtService = new JwtService(props);

        authService = new AuthenticationService(registry, otpService, jwtService, new SimpleMeterRegistry());
    }

    @Test
    void issuesTokenForValidNonMfaClient() {
        TokenResponse response = authService.authenticate(
                new TokenRequest("gateway-service", SERVICE_SECRET, null));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.roles()).containsExactly("USER");
    }

    @Test
    void rejectsWrongSecret() {
        assertThatThrownBy(() -> authService.authenticate(
                new TokenRequest("gateway-service", "wrong", null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsUnknownClient() {
        assertThatThrownBy(() -> authService.authenticate(
                new TokenRequest("nobody", "whatever", null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void requiresMfaForMfaEnabledClient() {
        assertThatThrownBy(() -> authService.authenticate(
                new TokenRequest("gateway-admin", ADMIN_SECRET, null)))
                .isInstanceOf(MfaRequiredException.class);
    }

    @Test
    void issuesTokenForMfaClientWithValidOtp() {
        String otp = otpService.generate(otpService.currentCounter());
        TokenResponse response = authService.authenticate(
                new TokenRequest("gateway-admin", ADMIN_SECRET, otp));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.roles()).contains("ADMIN", "USER");
    }
}
