package com.amol.microservices.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Event Streaming layer — a decoupled, high-throughput real-time backbone. Provides Kafka-style
 * topics with partitions and append-only offset logs, keyed partitioning, consumer groups with
 * positions and lag, a change-data-capture (CDC) ingestion endpoint, and event-bus fan-out.
 */
@SpringBootApplication
public class EventStreamingApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventStreamingApplication.class, args);
    }
}
