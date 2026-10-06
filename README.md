# razorpay

Payment gateway clone, split into microservices. The monolith it was split from is on `master` (removed from this branch in phase 7). Migration plan: [docs/microservices-migration.md](docs/microservices-migration.md).

## Services

| Service | Port | Role |
|---|---|---|
| api-gateway-service | 8080 | Only public entry point. Verifies API key / JWT, routes requests |
| merchant-service | 8081 | Merchants, users, API keys, customers, webhook configs |
| vault-service | 8082 | Card vault: encrypted PANs, tokens, card charge |
| payment-service | 8083 | Orders, payments, refunds, state machines, bank simulator |
| operations-service | 8084 | Nightly settlement, webhook delivery with retry and DLQ |
| discovery-service | 8761 | Eureka registry |
| config-service | 8888 | Spring Cloud Config (shared defaults) |

Infra: Postgres (one DB per service), Redis, Kafka, Zipkin, Prometheus.

## Run locally with Docker

```bash
cp .env.example .env          # then set VAULT_MASTER_KEY (openssl rand -base64 32) and JWT_SECRET_KEY
docker compose up --build
```

Gateway: `http://localhost:8080`. Zipkin: `http://localhost:9411`. Prometheus: `http://localhost:9090`.

## Build without Docker

Requires JDK 25 (`pom.xml` targets 25). To build with JDK 21 for local testing:

```bash
mvn -B -Djava.version=21 package     # from repo root, builds all modules
```

## Auth

- API key: `Authorization: Basic base64(keyId:secret)` on orders, payments, refunds, vault.
- Dashboard: `Authorization: Bearer <jwt>` from `POST /v1/auth/login`.
- Public: `POST /v1/auth/signup`, `POST /v1/auth/login`, `/webhook/**`.

The gateway strips `Authorization` and forwards `X-Merchant-Id` / `X-Key-Id` to services. Services sit on the private network and trust those headers.

## Service-to-service calls

Internal endpoints live under `/internal/v1/**` and are not routed by the gateway.

## Monitoring

| Tool | URL | Notes |
|---|---|---|
| Grafana | http://localhost:3000 | Dashboards in the Razorpay folder. Login admin / admin (default; see `GRAFANA_ADMIN_PASSWORD` in `.env.example`) |
| Confluent Control Center | http://localhost:9021 | Kafka topics, consumer lag, broker health |
| Prometheus | http://localhost:9090 | Scrapes `/actuator/prometheus` on each service |
| Zipkin | http://localhost:9411 | Traces |

## Smoke test

With the stack running:

```bash
bash scripts/smoke.sh      # BASE_URL defaults to http://localhost:8080
```

Covers signup, login, API key, tokenize, order, card payment, and gateway auth. Settlement and webhooks are not covered by the script.

## Tests

```bash
mvn -B -Djava.version=21 test    # unit and Testcontainers integration tests; needs Docker
```

## Kubernetes (kind)

Manifests in `k8s/` (kustomize, namespace `razorpay`). Secrets come from `k8s/secrets.env` (git-ignored; copy from `secrets.env.example`).

```bash
kind create cluster --name razorpay
docker compose build                       # images razorpay/<service>:latest
for s in discovery-service config-service merchant-service vault-service payment-service operations-service api-gateway-service; do
  kind load docker-image razorpay/$s:latest --name razorpay
done
kubectl apply -k k8s
kubectl -n razorpay port-forward svc/api-gateway-service 8080:8080
BASE_URL=http://localhost:8080 bash scripts/smoke.sh
```

Notes:
- Postgres (5Gi), Redis (1Gi, appendonly) and Kafka (10Gi) use PersistentVolumeClaims on the default StorageClass. Data survives pod restarts. Deleting the PVCs (`kubectl -n razorpay delete pvc --all`) wipes data.
- Postgres init scripts run only on an empty volume. The first apply after switching from `emptyDir` needs the app pods restarted so Flyway creates the schema.
- Pods set `enableServiceLinks: false`. Otherwise the Kubernetes `redis` service injects `REDIS_PORT=tcp://...` and Spring fails to start.
- Services register in Eureka by pod IP (`EUREKA_INSTANCE_PREFER_IP_ADDRESS`). Registering by pod hostname does not resolve across pods.
- The gateway's Eureka cache can stay stale after startup races. If the gateway returns 503 `Unable to find instance`, restart its deployment.
- Monitoring (Grafana, Prometheus, Control Center) is not in the cluster manifests yet.

