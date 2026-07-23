package com.amol.microservices.orchestration.integration;

/** Outcome of routing a canonical message: where it went and whether dispatch succeeded. */
public record RoutingResult(String destination, boolean dispatched, String detail) {
}
