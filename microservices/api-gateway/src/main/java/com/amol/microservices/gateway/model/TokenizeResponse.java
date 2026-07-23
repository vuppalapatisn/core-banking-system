package com.amol.microservices.gateway.model;

/** Opaque token that stands in for a sensitive value; safe to store and log. */
public record TokenizeResponse(String token) {
}
