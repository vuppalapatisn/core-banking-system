# payments-service — Payments Switch / Gateway

Accepts payment instructions, routes each to a network, and processes them idempotently.
Java 21 / Spring Boot 3.3.5, port **8100**, context-path **`/payments`**.

Content-based network routing: amount > 10,000.00 → `WIRE`; destination starting `EXT` → `ACH`;
otherwise `INTERNAL`. Re-submitting the same `idempotencyKey` returns the original payment instead
of creating a duplicate.

## Endpoints

| Method & path | Purpose |
|---------------|---------|
| `POST /payments/payments` | Submit a payment (`fromAccount`, `toAccount`, `amountMinor`, optional `idempotencyKey`) |
| `GET /payments/payments/{id}` | Get a payment |
| `GET /payments/swagger-ui.html` · `.../actuator/health` · `.../actuator/prometheus` | Docs / health / metrics |

```bash
cd microservices/payments-service && mvn clean package && docker build -t payments-service:local .
```
