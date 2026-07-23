# api-gateway — API Management & Security Layer

The platform's **API Management & Security** front door. A Java 21 / Spring Boot 3.3.5
service that authenticates and shields traffic before it reaches the backend microservices,
implementing each component of the target-architecture "API Management & Security Layer".

| Architecture component | Implementation |
|------------------------|----------------|
| **API Gateway / APIM** | Reverse proxy (`/route/{service}/**`) forwarding to `product`, `images`, `ecommerce` with correlation-id propagation |
| **OAuth2 / OIDC** | `POST /auth/token` issues signed HS256 JWTs; Spring Security resource server validates Bearer tokens |
| **MFA** | RFC 6238 TOTP second factor, required for MFA-enabled clients |
| **WAF** | Signature-based filter blocking SQLi / XSS / path-traversal (HTTP 403) |
| **Rate Limiting** | Per-client in-memory token bucket (HTTP 429 + `Retry-After`) |
| **IAM / RBAC** | Client identities + roles → JWT `roles` claim → Spring Security authorities |
| **Encryption & Tokenization** | AES-GCM tokenize / detokenize of sensitive values |

Cross-cutting concerns match the rest of the platform: **Micrometer/Prometheus metrics**,
**Spring Boot Actuator** health/probes, **structured JSON logs** (`logstash-logback-encoder`),
`X-Correlation-Id` propagation, **Swagger UI**, a **non-root** container image, and a
**memory-based HorizontalPodAutoscaler**.

## Endpoints

| Method & path | Auth | Purpose |
|---------------|------|---------|
| `POST /api-gateway/auth/token` | public | Issue a JWT (client credentials + MFA) |
| `ANY /api-gateway/route/{alias}/**` | Bearer JWT | Proxy to a backend service |
| `POST /api-gateway/security/tokenize` | Bearer JWT (any role) | Tokenize a sensitive value |
| `POST /api-gateway/security/detokenize` | Bearer JWT (`ROLE_ADMIN`) | Recover a value from a token |
| `GET /api-gateway/actuator/health` | public | Liveness/readiness |
| `GET /api-gateway/actuator/prometheus` | public | Metrics scrape |
| `GET /api-gateway/swagger-ui.html` | public | API docs |

Custom metrics: `gateway.auth.tokens`, `gateway.waf.blocked`, `gateway.ratelimit.throttled`,
`gateway.proxy.requests` (all tagged), alongside JVM heap and `http.server.requests` histograms.

## Configuration

All secrets come from environment variables / Kubernetes secrets — never hardcoded. Key settings
(`application.properties`, overridable via env):

| Property / env | Purpose |
|----------------|---------|
| `GATEWAY_SECURITY_JWT_SECRET` | HS256 signing secret (≥ 32 bytes) |
| `GATEWAY_TOKENIZATION_AES_KEY` | Base64 AES key (16/24/32 bytes) |
| `GATEWAY_SECURITY_CLIENTS_ADMIN_SECRET` / `..._SERVICE_SECRET` | Demo client secrets (BCrypt-hashed at startup) |
| `GATEWAY_SECURITY_MFA_TOTP_SEED` | Base32 TOTP seed for MFA |
| `GATEWAY_ROUTE_{PRODUCT,IMAGES,ECOMMERCE}` | Downstream base URLs |
| `GATEWAY_RATE_LIMIT_*`, `GATEWAY_WAF_ENABLED` | Rate limit / WAF tuning |
| `GATEWAY_CORS_ALLOWED_ORIGINS` | CORS allow-list (never `*` with credentials) |

> The defaults in `application.properties` are **development-only** placeholders. Supply real
> secrets via the `api-gateway-secret` Kubernetes Secret (see `k8s/api-gateway/secret-example.yaml`).

## Build & test

```bash
cd microservices/api-gateway
mvn clean package          # runs the test suite
docker build -t api-gateway:local .
```

## Try it locally

```bash
# 1. Get a token (non-MFA demo client)
curl -s -X POST http://localhost:8095/api-gateway/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"gateway-service","clientSecret":"<service secret>"}'

# 2. Call a backend through the gateway
curl http://localhost:8095/api-gateway/route/product/products \
  -H "Authorization: Bearer <token>"

# 3. Tokenize a value
curl -s -X POST http://localhost:8095/api-gateway/security/tokenize \
  -H "Authorization: Bearer <token>" -H 'Content-Type: application/json' \
  -d '{"value":"4111111111111111"}'
```

Redeploy just this service to the local cluster: `restart--redeploy-service.<sh|bat> api-gateway`.
