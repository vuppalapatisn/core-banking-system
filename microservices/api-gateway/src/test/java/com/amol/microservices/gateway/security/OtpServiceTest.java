package com.amol.microservices.gateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtpServiceTest {

    private final OtpService otpService = new OtpService("JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP");

    @Test
    void verifiesCurrentCode() {
        String current = otpService.generate(otpService.currentCounter());
        assertThat(otpService.verify(current)).isTrue();
    }

    @Test
    void acceptsCodeWithinSkewWindow() {
        String previousStep = otpService.generate(otpService.currentCounter() - 1);
        assertThat(otpService.verify(previousStep)).isTrue();
    }

    @Test
    void rejectsWrongCode() {
        String current = otpService.generate(otpService.currentCounter());
        String wrong = current.equals("000000") ? "111111" : "000000";
        assertThat(otpService.verify(wrong)).isFalse();
    }

    @Test
    void rejectsBlankOrMalformed() {
        assertThat(otpService.verify(null)).isFalse();
        assertThat(otpService.verify("")).isFalse();
        assertThat(otpService.verify("12345")).isFalse();
        assertThat(otpService.verify("abcdef")).isFalse();
    }
}
