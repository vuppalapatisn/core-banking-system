package com.amol.microservices.events.controller;

import com.amol.microservices.events.domain.EventRecord;
import com.amol.microservices.events.domain.Topic;
import com.amol.microservices.events.model.CdcRequest;
import com.amol.microservices.events.model.CreateTopicRequest;
import com.amol.microservices.events.model.PublishRequest;
import com.amol.microservices.events.service.EventBroker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Event streaming API — topics, publish, consumer-group poll, lag, and CDC ingestion. */
@RestController
@Tag(name = "Event Streaming", description = "Topics, partitions, publish/consume, and CDC")
public class EventController {

    private final EventBroker broker;

    public EventController(EventBroker broker) {
        this.broker = broker;
    }

    @PostMapping("/topics")
    @Operation(summary = "Create a topic (idempotent)")
    public ResponseEntity<Map<String, Object>> createTopic(@Valid @RequestBody CreateTopicRequest request) {
        Topic topic = broker.createTopic(request.name(), request.partitions());
        return ResponseEntity.ok(topicInfo(topic));
    }

    @GetMapping("/topics")
    @Operation(summary = "List topics")
    public ResponseEntity<List<Map<String, Object>>> listTopics() {
        return ResponseEntity.ok(broker.topics().stream().map(this::topicInfo).toList());
    }

    @PostMapping("/topics/{topic}/publish")
    @Operation(summary = "Publish an event to a topic (auto-creates the topic if absent)")
    public ResponseEntity<Map<String, Object>> publish(
            @PathVariable String topic, @Valid @RequestBody PublishRequest request) {
        EventRecord record = broker.publish(topic, request.key(), request.payload());
        return ResponseEntity.ok(published(record));
    }

    @GetMapping("/topics/{topic}/poll")
    @Operation(summary = "Poll the next batch for a consumer group (advances its position)")
    public ResponseEntity<Map<String, Object>> poll(
            @PathVariable String topic,
            @RequestParam String group,
            @RequestParam(defaultValue = "10") int max) {
        List<EventRecord> records = broker.poll(topic, group, max);
        Map<String, Object> body = new HashMap<>();
        body.put("group", group);
        body.put("count", records.size());
        body.put("records", records);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/topics/{topic}/lag")
    @Operation(summary = "Consumer-group lag for a topic")
    public ResponseEntity<Map<String, Object>> lag(@PathVariable String topic, @RequestParam String group) {
        return ResponseEntity.ok(Map.of("topic", topic, "group", group, "lag", broker.lag(topic, group)));
    }

    @PostMapping("/cdc")
    @Operation(summary = "Ingest a change-data-capture record",
            description = "Publishes to topic cdc.<entity>, keyed by the entity key so per-entity order is preserved.")
    public ResponseEntity<Map<String, Object>> cdc(@Valid @RequestBody CdcRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("changeType", request.changeType());
        payload.put("before", request.before());
        payload.put("after", request.after());
        EventRecord record = broker.publish("cdc." + request.entity(), request.key(), payload);
        return ResponseEntity.ok(published(record));
    }

    private Map<String, Object> topicInfo(Topic topic) {
        return Map.of(
                "name", topic.name(),
                "partitions", topic.partitionCount(),
                "retained", topic.retainedCount());
    }

    private Map<String, Object> published(EventRecord record) {
        return Map.of(
                "topic", record.topic(),
                "partition", record.partition(),
                "offset", record.offset());
    }
}
