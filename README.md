# core-banking-system

Reference services for an **AI-enabled digital bank**, each layer of the target architecture built
as a production-shaped Java 21 / Spring Boot 3.3.5 microservice. Every service ships with
Actuator health/probes, Micrometer/Prometheus metrics, structured JSON logging,
`X-Correlation-Id` propagation, Swagger UI, a non-root Docker image, and a memory-based
HorizontalPodAutoscaler.

## Services

| Service | Layer | Port | Context path |
|---------|-------|------|--------------|
| [`microservices/api-gateway`](microservices/api-gateway/) | API Management & Security | 8095 | `/api-gateway` |
| [`microservices/orchestration-service`](microservices/orchestration-service/) | Orchestration & Integration | 8096 | `/orchestration` |
| [`microservices/cbs-service`](microservices/cbs-service/) | Core Platforms — Core Banking | 8097 | `/cbs` |
| [`microservices/los-service`](microservices/los-service/) | Core Platforms — Loan Origination | 8098 | `/los` |
| [`microservices/lms-service`](microservices/lms-service/) | Core Platforms — Loan Management | 8099 | `/lms` |
| [`microservices/payments-service`](microservices/payments-service/) | Core Platforms — Payments | 8100 | `/payments` |

### API Management & Security ([details](microservices/api-gateway/README.md))

| Component | Implementation |
|-----------|----------------|
| API Gateway / APIM | `GatewayController` + `ProxyService` reverse proxy |
| OAuth2 / OIDC | `AuthController` issues HS256 JWTs; resource server validates |
| MFA | `OtpService` — RFC 6238 TOTP |
| WAF | `WafFilter` — SQLi / XSS / traversal → 403 |
| Rate Limiting | `RateLimitFilter` — token bucket → 429 |
| IAM / RBAC | `IamClientRegistry` + `SecurityConfig` |
| Encryption & Tokenization | `TokenizationService` — AES-GCM |

### Orchestration & Integration ([details](microservices/orchestration-service/README.md))

| Component | Implementation |
|-----------|----------------|
| BPM / Workflow | `WorkflowEngine` — loan-approval state machine |
| Business Rules Engine | `RulesEngine` — externalized, config-driven rules |
| Decision Orchestration | `DecisionOrchestrator` — combines rule sets by severity |
| ESB / Integration | `MessageRouter` — content-based routing of a canonical message |
| Service Mesh | `CircuitBreaker` + retry in code, **and** Istio manifests (`k8s/orchestration-service/service-mesh/`) |
| Microservices | each service, independently deployable |

### Core Platforms (systems of record)

| Service | Capability |
|---------|------------|
| **CBS** (`cbs-service`) | Customers, CASA accounts, and a double-entry general ledger |
| **LOS** (`los-service`) | Loan application intake → underwriting → origination |
| **LMS** (`lms-service`) | Loan servicing: amortization schedule, repayments, payoff |
| **Payments** (`payments-service`) | Payment submission, network routing (INTERNAL/ACH/WIRE), idempotency |

## Build & test

```bash
# per service
cd microservices/<service>          # api-gateway | orchestration-service
mvn clean package                   # runs the test suite
docker build -t <service>:local .
```

## Deploy (Kubernetes)

```bash
kubectl apply -f k8s/<service>/configmap.yaml
kubectl apply -f k8s/<service>/deployment.yaml
kubectl apply -f k8s/<service>/service.yaml
kubectl apply -f k8s/<service>/hpa.yaml
# api-gateway: provide real secrets first (see k8s/api-gateway/secret-example.yaml)
# orchestration-service: optional Istio mesh policy in k8s/orchestration-service/service-mesh/
```
