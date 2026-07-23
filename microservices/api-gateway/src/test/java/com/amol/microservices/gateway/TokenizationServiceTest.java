package com.amol.microservices.gateway;

import com.amol.microservices.gateway.config.GatewayProperties;
import com.amol.microservices.gateway.exception.InvalidCredentialsException;
import com.amol.microservices.gateway.service.TokenizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenizationServiceTest {

    private TokenizationService service;

    @BeforeEach
    void setUp() {
        GatewayProperties props = new GatewayProperties();
        // Valid 32-byte AES key (Base64).
        props.getTokenization().setAesKey("ZGV2LW9ubHktYWVzLTI1Ni1rZXktMzItYnl0ZXMhISE=");
        service = new TokenizationService(props);
    }

    @Test
    void tokenizeThenDetokenizeRoundTrips() {
        String secret = "4111111111111111";
        String token = service.tokenize(secret);

        assertThat(token).startsWith("tok_").doesNotContain(secret);
        assertThat(service.detokenize(token)).isEqualTo(secret);
    }

    @Test
    void sameInputProducesDifferentTokens() {
        // Random IV per call means identical plaintext never yields the same token.
        assertThat(service.tokenize("same")).isNotEqualTo(service.tokenize("same"));
    }

    @Test
    void detokenizeRejectsMalformedToken() {
        assertThatThrownBy(() -> service.detokenize("not-a-token"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detokenizeRejectsForgedToken() {
        // Valid prefix + Base64 but wrong bytes → GCM integrity check must fail.
        assertThatThrownBy(() -> service.detokenize("tok_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void invalidKeyLengthIsRejectedAtStartup() {
        GatewayProperties bad = new GatewayProperties();
        bad.getTokenization().setAesKey("c2hvcnQ="); // "short" -> 5 bytes
        assertThatThrownBy(() -> new TokenizationService(bad))
                .isInstanceOf(IllegalStateException.class);
    }
}
