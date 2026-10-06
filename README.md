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
