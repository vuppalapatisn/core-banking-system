package com.amol.microservices.cbs.model;

/** Uniform error body — stable machine-readable {@code error} code plus a safe {@code message}. */
public record ErrorResponse(String error, String message) {
}
