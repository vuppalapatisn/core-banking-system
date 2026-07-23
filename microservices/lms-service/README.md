# lms-service — Loan Management System

Services loans post-disbursement. Java 21 / Spring Boot 3.3.5, port **8099**, context-path **`/lms`**.

Computes a fixed-EMI **amortization schedule** (reducing-balance; `EMI = P·r·(1+r)ⁿ / ((1+r)ⁿ−1)`,
with a zero-rate fallback), records repayments against the outstanding balance, and marks the loan
`PAID_OFF` at zero. Money in minor units.

## Endpoints

| Method & path | Purpose |
|---------------|---------|
| `POST /lms/loans` | Book a loan (`{"principalMinor":100000,"annualRatePct":12.0,"termMonths":12}`) |
| `GET /lms/loans/{id}` | Get a loan + outstanding balance |
| `GET /lms/loans/{id}/schedule` | Amortization schedule |
| `POST /lms/loans/{id}/payments` | Record a repayment (409 if already paid off) |
| `GET /lms/swagger-ui.html` · `.../actuator/health` · `.../actuator/prometheus` | Docs / health / metrics |

```bash
cd microservices/lms-service && mvn clean package && docker build -t lms-service:local .
```
