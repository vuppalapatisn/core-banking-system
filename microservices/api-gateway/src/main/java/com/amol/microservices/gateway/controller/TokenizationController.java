package com.amol.microservices.gateway.controller;

import com.amol.microservices.gateway.model.DetokenizeRequest;
import com.amol.microservices.gateway.model.DetokenizeResponse;
import com.amol.microservices.gateway.model.TokenizeRequest;
import com.amol.microservices.gateway.model.TokenizeResponse;
import com.amol.microservices.gateway.service.TokenizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Encryption &amp; tokenization endpoints. {@code /tokenize} requires any authenticated client;
 * {@code /detokenize} (recovering plaintext) requires {@code ROLE_ADMIN} (enforced in SecurityConfig).
 */
@RestController
@RequestMapping("/security")
@Tag(name = "Tokenization", description = "AES-GCM tokenization of sensitive values")
public class TokenizationController {

    private final TokenizationService tokenizationService;

    public TokenizationController(TokenizationService tokenizationService) {
        this.tokenizationService = tokenizationService;
    }

    @PostMapping("/tokenize")
    @Operation(summary = "Tokenize a sensitive value into an opaque token")
    public ResponseEntity<TokenizeResponse> tokenize(@Valid @RequestBody TokenizeRequest request) {
        return ResponseEntity.ok(new TokenizeResponse(tokenizationService.tokenize(request.value())));
    }

    @PostMapping("/detokenize")
    @Operation(summary = "Recover the original value from a token (ADMIN only)")
    public ResponseEntity<DetokenizeResponse> detokenize(@Valid @RequestBody DetokenizeRequest request) {
        return ResponseEntity.ok(new DetokenizeResponse(tokenizationService.detokenize(request.token())));
    }
}
