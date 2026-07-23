package com.amol.microservices.events.service;

import com.amol.microservices.events.domain.EventRecord;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventBrokerTest {

    private EventBroker broker;

    @BeforeEach
    void setUp() {
        broker = new EventBroker(3, 10_000, new SimpleMeterRegistry());
    }

    @Test
    void sameKeyGoesToSamePartitionWithIncrementingOffsets() {
        EventRecord first = broker.publish("orders", "k1", Map.of("n", 1));
        EventRecord second = broker.publish("orders", "k1", Map.of("n", 2));
        assertThat(second.partition()).isEqualTo(first.partition());
        assertThat(first.offset()).isZero();
        assertThat(second.offset()).isEqualTo(1);
    }

    @Test
    void pollReturnsThenAdvances() {
        broker.publish("t", "k", Map.of("n", 1));
        broker.publish("t", "k", Map.of("n", 2));
        assertThat(broker.poll("t", "g", 10)).hasSize(2);
        assertThat(broker.poll("t", "g", 10)).isEmpty(); // position advanced
    }

    @Test
    void consumerGroupsConsumeIndependently() {
        broker.publish("t", "k", Map.of("n", 1));
        assertThat(broker.poll("t", "g1", 10)).hasSize(1);
        assertThat(broker.poll("t", "g2", 10)).hasSize(1); // fan-out: g2 sees it too
    }

    @Test
    void lagReflectsUnconsumedRecords() {
        broker.publish("t", "k", Map.of("n", 1));
        broker.publish("t", "k", Map.of("n", 2));
        assertThat(broker.lag("t", "g")).isEqualTo(2);
        broker.poll("t", "g", 10);
        assertThat(broker.lag("t", "g")).isZero();
    }

    @Test
    void retentionTrimsOldestButKeepsOffsetsMonotonic() {
        EventBroker small = new EventBroker(1, 2, new SimpleMeterRegistry());
        small.publish("t", "k", Map.of("n", 1));
        small.publish("t", "k", Map.of("n", 2));
        EventRecord third = small.publish("t", "k", Map.of("n", 3));
        assertThat(third.offset()).isEqualTo(2);
        List<EventRecord> polled = small.poll("t", "g", 10);
        assertThat(polled).hasSize(2); // oldest (offset 0) trimmed
        assertThat(polled.get(0).offset()).isEqualTo(1);
    }

    @Test
    void pollUnknownTopicIsRejected() {
        assertThatThrownBy(() -> broker.poll("nope", "g", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
