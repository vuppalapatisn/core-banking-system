package com.amol.microservices.gateway.security;

import com.amol.microservices.gateway.config.GatewayProperties;
import com.amol.microservices.gateway.model.TokenResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Issues OAuth2-style HS256 JWT access tokens. The same secret is used by the resource-server
 * {@code JwtDecoder} (see {@code SecurityConfig}) to validate incoming Bearer tokens.
 */
@Service
public class JwtService {

    /** HS256 requires a key of at least 256 bits (32 bytes). */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtEncoder encoder;
    private final GatewayProperties.Jwt jwtProps;

    public JwtService(GatewayProperties properties) {
        this.jwtProps = properties.getSecurity().getJwt();
        byte[] keyBytes = requireStrongSecret(jwtProps.getSecret());
        SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    private static byte[] requireStrongSecret(String secret) {
        if (secret == null) {
            throw new IllegalStateException("gateway.security.jwt.secret must be configured");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "gateway.security.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
        return bytes;
    }

    /**
     * Issues a signed access token for {@code subject} carrying the granted {@code roles}
     * (the RBAC scopes) as the {@code roles} claim.
     */
    public TokenResponse issue(String subject, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProps.getIssuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(jwtProps.getTtlSeconds()))
                .subject(subject)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", jwtProps.getTtlSeconds(), roles);
    }
}
