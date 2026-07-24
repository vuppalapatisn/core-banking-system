# Deploying the platform in-cluster

Deploys all 8 services into the `ecommerce` namespace and runs the end-to-end loan journey with
correlated logs across pods.

## Prerequisites

- A Kubernetes cluster + `kubectl` (Docker Desktop, kind, minikube, or a managed cluster).
- Container images. Either:
  - **From CI** — the `ci.yaml` workflow publishes `docker.io/<DOCKER_USERNAME>/<service>:latest`
    (the deployments reference `docker.io/sudhavuppalapati/<service>:latest` — edit to your registry), or
  - **Local (Docker Desktop)** — build each image locally and switch the deployments to not pull:
    ```bash
    for s in api-gateway orchestration-service cbs-service los-service lms-service \
             payments-service event-streaming-service ai-intelligence-service; do
      (cd microservices/$s && mvn -q clean package -DskipTests && docker build -t $s:local .)
    done
    # then set image/pull-policy per deployment, or use: kubectl set image + imagePullPolicy=IfNotPresent
    ```

## Deploy

```bash
kubectl apply -k k8s/
kubectl -n ecommerce rollout status deploy/event-streaming-service
kubectl -n ecommerce rollout status deploy/cbs-service deploy/los-service deploy/lms-service deploy/payments-service
kubectl -n ecommerce rollout status deploy/orchestration-service deploy/ai-intelligence-service deploy/api-gateway
kubectl -n ecommerce get pods
```

> Excluded from the kustomization on purpose: `api-gateway/secret-example.yaml` (create the real
> `api-gateway-secret` out-of-band) and `orchestration-service/service-mesh/` (apply only if Istio is
> installed). HPAs are included but inert until a metrics-server is present.

## Drive the end-to-end loan journey

```bash
kubectl -n ecommerce port-forward svc/orchestration-service 8096:8096
```

```bash
curl -s -X POST http://localhost:8096/orchestration/journeys/loan \
  -H 'Content-Type: application/json' -H 'X-Correlation-Id: 11111111-1111-1111-1111-111111111111' \
  -d '{"applicantId":"c1","amountMinor":100000,"termMonths":12,"creditScore":720,"annualRatePct":12.0,"toAccount":"ACC-2"}'
# -> {"applicationStatus":"ORIGINATED","loanId":"...","scheduleMonths":12,"paymentStatus":"SETTLED","outcome":"DISBURSED"}
```

The orchestration service calls LOS → LMS → Payments, and each hop also emits a domain event to the
event-streaming-service.

## See correlated logs across pods

Every service logs structured JSON with the `correlationId`. Trace one journey across all pods:

```bash
kubectl -n ecommerce logs -l 'app in (orchestration-service,los-service,lms-service,payments-service)' \
  --prefix --tail=-1 | grep 11111111-1111-1111-1111-111111111111
```

Inspect what the streaming layer captured (e.g. payment events):

```bash
kubectl -n ecommerce port-forward svc/event-streaming-service 8101:8101
curl "http://localhost:8101/events/topics/payments.payment/poll?group=demo&max=10"
```

## Teardown

```bash
kubectl delete -k k8s/
```
