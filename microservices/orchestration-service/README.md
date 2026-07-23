# orchestration-service — Orchestration & Integration Layer

The "traffic control" layer of the digital-bank platform: it coordinates how systems talk and how
business processes flow. Java 21 / Spring Boot 3.3.5, port **8096**, context-path **`/orchestration`**.

| Component | Implementation |
|-----------|----------------|
| **BPM / Workflow** | [`WorkflowEngine`](src/main/java/com/amol/microservices/orchestration/workflow/WorkflowEngine.java) drives a loan-approval state machine (SUBMITTED → APPROVED → DISBURSED, or MANUAL_REVIEW / REJECTED) |
| **Business Rules Engine** | [`RulesEngine`](src/main/java/com/amol/microservices/orchestration/rules/RulesEngine.java) evaluates **externalized** rules from `application.properties` — change credit/risk policy with no code deploy |
| **Decision Orchestration** | [`DecisionOrchestrator`](src/main/java/com/amol/microservices/orchestration/decision/DecisionOrchestrator.java) runs multiple rule sets ("models") and combines by severity: REJECT > REVIEW > APPROVE |
| **ESB / Integration** | [`MessageRouter`](src/main/java/com/amol/microservices/orchestration/integration/MessageRouter.java) does content-based routing of a canonical message to a configured destination |
| **Service Mesh** | [`CircuitBreaker`](src/main/java/com/amol/microservices/orchestration/integration/CircuitBreaker.java) + retry in [`ResilientDispatcher`](src/main/java/com/amol/microservices/orchestration/integration/ResilientDispatcher.java), **and** Istio manifests in [`k8s/orchestration-service/service-mesh/`](../../k8s/orchestration-service/service-mesh/) |
| **Microservices** | this service, independently buildable and deployable |

## Endpoints

| Method & path | Purpose |
|---------------|---------|
| `POST /orchestration/workflows` | Start a workflow instance (`{"type":"loan-approval","facts":{...}}`) |
| `POST /orchestration/workflows/{id}/advance` | Advance the instance one step |
| `GET /orchestration/workflows/{id}` | Inspect an instance |
| `POST /orchestration/rules/evaluate` | Evaluate a rule set against facts |
| `GET /orchestration/rules` · `GET /orchestration/rules/{set}` | List rule sets / rules |
| `POST /orchestration/decisions` | Orchestrate a decision across rule sets |
| `POST /orchestration/integration/route` | Content-based route a canonical message |
| `GET /orchestration/actuator/health` · `.../prometheus` | Health / metrics |
| `GET /orchestration/swagger-ui.html` | API docs |

Custom metrics: `orchestration.workflows`, `orchestration.decisions`, `orchestration.messages.routed`.

## Example: run a loan through the process

```bash
BASE=http://localhost:8096/orchestration

# 1. Start
ID=$(curl -s -X POST $BASE/workflows -H 'Content-Type: application/json' \
  -d '{"type":"loan-approval","facts":{"creditScore":720,"debtToIncome":0.2,"creditHistoryYears":5,"amount":5000}}' \
  | sed -E 's/.*"id":"([^"]+)".*/\1/')

# 2. Advance: decisioning -> APPROVED
curl -s -X POST $BASE/workflows/$ID/advance

# 3. Advance: disbursement (ESB route) -> DISBURSED / COMPLETED
curl -s -X POST $BASE/workflows/$ID/advance
```

## Business rules are externalized

Credit and risk policy live in `application.properties` under `orchestration.rules.*` and can be
overridden per environment via the ConfigMap — no rebuild required. Example:

```properties
orchestration.rules.credit[0].name=reject-low-credit-score
orchestration.rules.credit[0].attribute=creditScore
orchestration.rules.credit[0].operator=LT
orchestration.rules.credit[0].value=600
orchestration.rules.credit[0].outcome=REJECT
```

## Build & test

```bash
cd microservices/orchestration-service
mvn clean package                 # runs the test suite (27 tests)
docker build -t orchestration-service:local .
```
