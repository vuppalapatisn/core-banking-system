# cbs-service — Core Banking System

System of record for customers, CASA accounts, and general-ledger postings.
Java 21 / Spring Boot 3.3.5, port **8097**, context-path **`/cbs`**.

Every money movement posts to a **double-entry general ledger** (the account leg plus a contra
`GL-CASH` leg) and balances are held in **minor units** (`long`) to avoid floating-point money errors.

## Endpoints

| Method & path | Purpose |
|---------------|---------|
| `POST /cbs/customers` | Onboard a customer |
| `GET /cbs/customers/{id}` | Get a customer |
| `POST /cbs/accounts` | Open a CASA account (`CURRENT`/`SAVINGS`) |
| `GET /cbs/accounts/{id}` | Get an account + balance |
| `POST /cbs/accounts/{id}/deposit` | Deposit (`{"amountMinor":10000}`) |
| `POST /cbs/accounts/{id}/withdraw` | Withdraw (409 on insufficient funds) |
| `GET /cbs/accounts/{id}/ledger` | Ledger postings for an account |
| `GET /cbs/swagger-ui.html` · `.../actuator/health` · `.../actuator/prometheus` | Docs / health / metrics |

```bash
cd microservices/cbs-service && mvn clean package && docker build -t cbs-service:local .
```
