package com.amol.microservices.gateway.controller;

import com.amol.microservices.gateway.model.TokenRequest;
import com.amol.microservices.gateway.model.TokenResponse;
import com.amol.microservices.gateway.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OAuth2/OIDC-style token endpoint. Exchanges client credentials (plus MFA when required) for a JWT. */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "OAuth2/JWT token issuance with optional MFA")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/token")
    @SecurityRequirements // public endpoint — no bearer token required
    @Operation(summary = "Issue an access token",
            description = "Validates client credentials and, for MFA-enabled clients, a TOTP one-time code. "
                    + "Returns a signed JWT carrying the client's RBAC roles.")
    public ResponseEntity<TokenResponse> token(@Valid @RequestBody TokenRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
}
