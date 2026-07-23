# core-banking-system — API Management & Security Layer

The **API Management & Security** front door for an AI-enabled digital bank. A Java 21 /
Spring Boot 3.3.5 service that authenticates and shields traffic before it reaches the backend
banking microservices, implementing each component of the target-architecture "API Management &
Security Layer".

> Service source: [`microservices/api-gateway/`](microservices/api-gateway/) ·
> Kubernetes manifests: [`k8s/api-gateway/`](k8s/api-gateway/) ·
> Full service docs: [`microservices/api-gateway/README.md`](microservices/api-gateway/README.md)

## Architecture mapping

| Architecture component | Implementation |
|------------------------|----------------|
| **API Gateway / APIM** | [`GatewayController`](microservices/api-gateway/src/main/java/com/amol/microservices/gateway/controller/GatewayController.java) + [`ProxyService`](microservices/api-gateway/src/main/java/com/amol/microservices/gateway/service/ProxyService.java) reverse-proxy to backend services |
| **OAuth2 / OIDC** | [`AuthController`](microservices/api-gateway/src/main/java/com/amol/microservices/gateway/controller/AuthController.java) issues HS256 JWTs; Spring Security resource server validates them |
| **MFA** | [`OtpService`](microservices/api-gateway/src/main/java/com/amol/microservices/gateway/security/OtpService.java) — real RFC 6238 TOTP |
| **WAF** | [`WafFilter`](microservices/api-gateway/src/main/java/com/amol/microservices/gateway/filter/WafFilter.java) — SQLi / XSS / path-traversal → HTTP 403 |
| **Rate Limiting** | [`RateLimitFilter`](microservices/api-gateway/src/main/java/com/amol/microservices/gateway/filter/RateLimitFilter.java) — per-client token bucket → HTTP 429 |
| **IAM / RBAC** | [`IamClientRegistry`](microservices/api-gateway/src/main/java/com/amol/microservices/gateway/security/IamClientRegistry.java) + roles in JWT → [`SecurityConfig`](microservices/api-gateway/src/main/java/com/amol/microservices/gateway/config/SecurityConfig.java) |
| **Encryption & Tokenization** | [`TokenizationService`](microservices/api-gateway/src/main/java/com/amol/microservices/gateway/service/TokenizationService.java) — AES-GCM |

Cross-cutting: Micrometer/Prometheus metrics, Spring Boot Actuator health/probes, structured JSON
logs, `X-Correlation-Id` propagation, Swagger UI, a non-root container image, and a memory-based
HorizontalPodAutoscaler.

## Build & test

```bash
cd microservices/api-gateway
mvn clean package                 # runs the test suite
docker build -t api-gateway:local .
```

Runs on port **8095** under context-path **`/api-gateway`**. Swagger UI:
`http://localhost:8095/api-gateway/swagger-ui.html`.

## Deploy (Kubernetes)

```bash
# Provide real secrets first (see k8s/api-gateway/secret-example.yaml)
kubectl apply -f k8s/api-gateway/configmap.yaml
kubectl apply -f k8s/api-gateway/deployment.yaml
kubectl apply -f k8s/api-gateway/service.yaml
kubectl apply -f k8s/api-gateway/hpa.yaml
```
