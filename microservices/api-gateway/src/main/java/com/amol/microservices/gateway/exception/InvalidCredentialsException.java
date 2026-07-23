package com.amol.microservices.gateway.exception;

/** Raised when client credentials or the MFA code are invalid. Maps to HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
