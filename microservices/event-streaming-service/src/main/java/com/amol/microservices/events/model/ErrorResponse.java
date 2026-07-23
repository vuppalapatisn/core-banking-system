package com.amol.microservices.events.model;

/** Uniform error body — stable machine-readable {@code error} code plus a safe {@code message}. */
public record ErrorResponse(String error, String message) {
}
