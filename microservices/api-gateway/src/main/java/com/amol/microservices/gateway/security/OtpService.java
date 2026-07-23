package com.amol.microservices.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Multi-factor authentication second factor: verifies RFC 6238 TOTP codes (the same scheme used
 * by Google Authenticator / Authy). A ±1 time-step window is allowed to tolerate clock skew.
 *
 * <p>The shared TOTP seed (Base32) is supplied via configuration/secret so it can be provisioned
 * into an authenticator app; it is never logged.
 */
@Service
public class OtpService {

    private static final long TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int SKEW_STEPS = 1;

    private final byte[] seed;

    public OtpService(
            @Value("${gateway.security.mfa.totp-seed:JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP}") String base32Seed) {
        this.seed = base32Decode(base32Seed);
    }

    /** @return true if {@code otp} is a valid current TOTP code (within the skew window). */
    public boolean verify(String otp) {
        if (otp == null || !otp.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        long counter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (long offset = -SKEW_STEPS; offset <= SKEW_STEPS; offset++) {
            if (generate(counter + offset).equals(otp)) {
                return true;
            }
        }
        return false;
    }

    /** Current TOTP time-step counter. Package-private for testing. */
    long currentCounter() {
        return System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
    }

    /** Generates the TOTP code for a given time-step counter. Package-private for testing. */
    String generate(long counter) {
        try {
            byte[] data = ByteBuffer.allocate(Long.BYTES).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(seed, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int idx = hash[hash.length - 1] & 0xf;
            int binary = ((hash[idx] & 0x7f) << 24)
                    | ((hash[idx + 1] & 0xff) << 16)
                    | ((hash[idx + 2] & 0xff) << 8)
                    | (hash[idx + 3] & 0xff);
            int code = binary % (int) Math.pow(10, DIGITS);
            return String.format(Locale.ROOT, "%0" + DIGITS + "d", code);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            // HmacSHA1 is guaranteed present on every JVM; treat any failure as a non-match.
            throw new IllegalStateException("TOTP generation failed", ex);
        }
    }

    /** Minimal RFC 4648 Base32 decoder (no padding required). */
    private static byte[] base32Decode(String input) {
        String cleaned = input.trim().replace("=", "").toUpperCase(Locale.ROOT);
        if (cleaned.isEmpty()) {
            throw new IllegalStateException("gateway.security.mfa.totp-seed must not be blank");
        }
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        int buffer = 0;
        int bitsLeft = 0;
        byte[] out = new byte[cleaned.length() * 5 / 8];
        int outIndex = 0;
        for (int i = 0; i < cleaned.length(); i++) {
            int val = alphabet.indexOf(cleaned.charAt(i));
            if (val < 0) {
                throw new IllegalStateException("Invalid Base32 character in TOTP seed");
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                out[outIndex++] = (byte) ((buffer >> bitsLeft) & 0xff);
            }
        }
        return out;
    }
}
