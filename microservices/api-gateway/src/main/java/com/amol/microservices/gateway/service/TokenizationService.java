package com.amol.microservices.gateway.service;

import com.amol.microservices.gateway.config.GatewayProperties;
import com.amol.microservices.gateway.exception.InvalidCredentialsException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption &amp; tokenization: replaces a sensitive value (card/account number) with an opaque,
 * reversible token using AES-GCM (authenticated encryption). A fresh random IV is generated per
 * call and prepended to the ciphertext, so identical inputs never produce the same token.
 *
 * <p>The AES key is supplied via configuration/secret (Base64). Plaintext values are never logged.
 */
@Service
public class TokenizationService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String TOKEN_PREFIX = "tok_";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenizationService(GatewayProperties properties) {
        String base64Key = properties.getTokenization().getAesKey();
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("gateway.tokenization.aes-key must be configured");
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException(
                    "gateway.tokenization.aes-key must decode to 16, 24, or 32 bytes (AES-128/192/256)");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /** Encrypts {@code value} into an opaque {@code tok_...} token. */
    public String tokenize(String value) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] combined = ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
            return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Tokenization failed", ex);
        }
    }

    /** Recovers the original value from a {@code tok_...} token. Rejects malformed/forged tokens. */
    public String detokenize(String token) {
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            throw new IllegalArgumentException("Malformed token");
        }
        try {
            byte[] combined = Base64.getUrlDecoder().decode(token.substring(TOKEN_PREFIX.length()));
            if (combined.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Malformed token");
            }
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (GeneralSecurityException ex) {
            // GCM tag mismatch — the token was forged or tampered with.
            throw new InvalidCredentialsException("Token failed integrity verification");
        }
    }
}
