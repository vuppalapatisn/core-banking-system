# los-service — Loan Origination System

Manages the loan application lifecycle. Java 21 / Spring Boot 3.3.5, port **8098**, context-path **`/los`**.

Lifecycle: `RECEIVED` → (underwrite) → `APPROVED` / `REJECTED` → (originate) → `ORIGINATED`.
Underwriting policy: credit score ≥ 640 **and** amount ≤ 50,000.00, else rejected. Illegal
transitions (e.g. originate before approval) return HTTP 409.

## Endpoints

| Method & path | Purpose |
|---------------|---------|
| `POST /los/applications` | Submit an application |
| `GET /los/applications/{id}` | Get an application |
| `POST /los/applications/{id}/underwrite` | Underwrite → APPROVED / REJECTED |
| `POST /los/applications/{id}/originate` | Originate (book) an approved application |
| `GET /los/swagger-ui.html` · `.../actuator/health` · `.../actuator/prometheus` | Docs / health / metrics |

```bash
cd microservices/los-service && mvn clean package && docker build -t los-service:local .
```
