# event-streaming-service — Event Streaming Layer

A decoupled, high-throughput real-time backbone with **Kafka-style semantics**, implemented
self-contained (no external broker). Java 21 / Spring Boot 3.3.5, port **8101**, context-path **`/events`**.

- **Topics + partitions** — each partition is an append-only, offset-addressed log.
- **Keyed partitioning** — records with the same key land on the same partition (per-key ordering);
  unkeyed records spread round-robin for throughput.
- **Consumer groups** — each group tracks its own position per partition, so different groups
  independently consume the whole stream (**event-bus fan-out**); `lag` reports un-consumed records.
- **Retention** — size-based per partition; trimming advances the base offset so offsets stay monotonic.
- **CDC** — a change-data-capture endpoint publishes to `cdc.<entity>`, keyed by entity id.

> Reference implementation of the streaming concepts. In production the `EventBroker` is backed by
> **Kafka / Azure Event Hub**; the REST contract and semantics stay the same.

## Endpoints

| Method & path | Purpose |
|---------------|---------|
| `POST /events/topics` | Create a topic (`{"name":"orders","partitions":3}`, idempotent) |
| `GET /events/topics` | List topics (name, partitions, retained count) |
| `POST /events/topics/{topic}/publish` | Publish an event (`{"key":"k1","payload":{...}}`; auto-creates topic) |
| `GET /events/topics/{topic}/poll?group=g&max=10` | Poll next batch for a group (advances its position) |
| `GET /events/topics/{topic}/lag?group=g` | Consumer-group lag |
| `POST /events/cdc` | Ingest a CDC record (`{"entity":"account","changeType":"UPDATE","key":"a1","after":{...}}`) |
| `GET /events/swagger-ui.html` · `.../actuator/health` · `.../actuator/prometheus` | Docs / health / metrics |

Metrics: `events.published{topic}`, `events.consumed{topic,group}`.

```bash
cd microservices/event-streaming-service && mvn clean package && docker build -t event-streaming-service:local .
```
