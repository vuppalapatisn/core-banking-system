package com.amol.microservices.events.domain;

/** An immutable event as stored in a partition log, addressed by (topic, partition, offset). */
public record EventRecord(
        String topic,
        int partition,
        long offset,
        String key,
        Object payload,
        long timestampMs) {
}
