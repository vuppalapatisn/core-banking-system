package com.amol.microservices.gateway.exception;

/** Raised when a valid MFA (OTP) second factor is required but was missing. Maps to HTTP 401. */
public class MfaRequiredException extends RuntimeException {
    public MfaRequiredException(String message) {
        super(message);
    }
}
