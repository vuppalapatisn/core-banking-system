package com.amol.microservices.events.service;

import com.amol.microservices.events.domain.EventRecord;
import com.amol.microservices.events.domain.Partition;
import com.amol.microservices.events.domain.Topic;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Kafka-style event broker. Manages topics/partitions and tracks per-consumer-group read
 * positions so different groups independently consume the full stream (event-bus fan-out). This is a
 * self-contained reference implementation; a production deployment backs it with Kafka / Event Hub.
 */
@Service
public class EventBroker {

    private static final Logger log = LoggerFactory.getLogger(EventBroker.class);

    private final int defaultPartitions;
    private final int retention;
    private final MeterRegistry meterRegistry;

    private final ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();
    /** topic::group -> (partition -> next offset to read). */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, Long>> positions = new ConcurrentHashMap<>();

    public EventBroker(
            @Value("${events.default-partitions:3}") int defaultPartitions,
            @Value("${events.retention-per-partition:10000}") int retention,
            MeterRegistry meterRegistry) {
        this.defaultPartitions = defaultPartitions;
        this.retention = retention;
        this.meterRegistry = meterRegistry;
    }

    /** Creates a topic (idempotent — returns the existing topic if already present). */
    public Topic createTopic(String name, Integer partitions) {
        int count = partitions == null || partitions <= 0 ? defaultPartitions : partitions;
        return topics.computeIfAbsent(name, n -> {
            log.info("topic_created name={} partitions={}", n, count);
            return new Topic(n, count, retention);
        });
    }

    public Topic get(String name) {
        Topic topic = topics.get(name);
        if (topic == null) {
            throw new IllegalArgumentException("Unknown topic: " + name);
        }
        return topic;
    }

    public List<Topic> topics() {
        return new ArrayList<>(topics.values());
    }

    /** Publishes to a topic, auto-creating it with default partitions if it does not yet exist. */
    public EventRecord publish(String topicName, String key, Object payload) {
        Topic topic = topics.computeIfAbsent(topicName, n -> new Topic(n, defaultPartitions, retention));
        EventRecord record = topic.publish(key, payload, System.currentTimeMillis());
        Counter.builder("events.published").tag("topic", topicName).register(meterRegistry).increment();
        return record;
    }

    /** Reads the next batch for a consumer group and advances its position (at-most-once). */
    public List<EventRecord> poll(String topicName, String group, int max) {
        Topic topic = get(topicName);
        Map<Integer, Long> pos = positions.computeIfAbsent(topicName + "::" + group, k -> new ConcurrentHashMap<>());
        List<EventRecord> out = new ArrayList<>();
        for (Partition p : topic.partitions()) {
            if (out.size() >= max) {
                break;
            }
            long position = Math.max(pos.getOrDefault(p.id(), p.baseOffset()), p.baseOffset());
            List<EventRecord> batch = p.read(position, max - out.size());
            if (!batch.isEmpty()) {
                out.addAll(batch);
                pos.put(p.id(), batch.get(batch.size() - 1).offset() + 1);
            } else {
                pos.putIfAbsent(p.id(), position);
            }
        }
        if (!out.isEmpty()) {
            Counter.builder("events.consumed").tag("topic", topicName).tag("group", group)
                    .register(meterRegistry).increment(out.size());
        }
        return out;
    }

    /** Total un-consumed records for a group across all partitions. */
    public long lag(String topicName, String group) {
        Topic topic = get(topicName);
        Map<Integer, Long> pos = positions.getOrDefault(topicName + "::" + group, new ConcurrentHashMap<>());
        long lag = 0;
        for (Partition p : topic.partitions()) {
            long position = Math.max(pos.getOrDefault(p.id(), p.baseOffset()), p.baseOffset());
            lag += p.highWatermark() - position;
        }
        return lag;
    }
}
