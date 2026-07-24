# ai-intelligence-service — AI/ML & Intelligence Layer

Inference services for the intelligence layer. Java 21 / Spring Boot 3.3.5, port **8102**, context-path **`/ai`**.

Models are lightweight, deterministic reference implementations so the service is self-contained and
**test-safe (no external ML infra, no paid LLM calls)**. Each sits behind a seam a trained model /
real LLM swaps into for production.

| Capability | Endpoint | Model |
|-----------|----------|-------|
| Fraud detection | `POST /ai/fraud/score` | weighted signals (amount, cross-border, rail, velocity) → 0..1 + ALLOW/REVIEW/BLOCK |
| Credit scoring | `POST /ai/credit/score` | 300..850 scorecard from income/debt/delinquencies/utilization + band |
| Churn prediction | `POST /ai/churn/predict` | logistic model over engagement features → probability + LOW/MEDIUM/HIGH |
| Segmentation + NBO | `POST /ai/segment` | segment (HIGH_VALUE/MASS_AFFLUENT/STANDARD/DORMANT) + next-best-offer |
| GenAI assistant | `POST /ai/assistant/ask` | pluggable `AssistantModel` (deterministic default; real LLM swaps in) |

`GET /ai/swagger-ui.html` · `.../actuator/health` · `.../actuator/prometheus`. Fraud decisions are
counted in `ai.fraud.decisions`.

```bash
curl -X POST http://localhost:8102/ai/fraud/score -H 'Content-Type: application/json' \
  -d '{"amountMinor":600000,"network":"WIRE","country":"US","homeCountry":"GB","recentTxnCount":20}'
# -> {"score":1.0,"decision":"BLOCK","reasons":["large amount","cross-border transaction","wire rail","high transaction velocity"]}
```

```bash
cd microservices/ai-intelligence-service && mvn clean package && docker build -t ai-intelligence-service:local .
```
