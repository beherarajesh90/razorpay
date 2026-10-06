# Razorpay monolith → microservices migration

Branch: `microservices` (in this repo; `master` keeps the monolith).

## Decisions

- **Repo:** same repo, new branch. Keeps git history of the monolith; each phase is one commit.
- **Build:** Maven multi-module reactor at repo root. Parent manages Spring Boot 4.1.0 and Spring Cloud 2025.1.2 versions.
- **Packages:** keep `com.systemdesign.razorpay.*` so moved classes need few import edits.
- **Database:** one Postgres instance, one database per service (`merchant_db`, `payment_db`, `vault_db`, `operations_db`). No cross-service joins or shared tables.
- **Cross-service calls:** synchronous HTTP via OpenFeign (lookups, tokenize, charge). Async via Kafka outbox (payment/refund/settlement events).
- **Service discovery:** Eureka. **Config:** Spring Cloud Config, classpath/native profile (no secrets in git).
- **Edge:** Spring Cloud Gateway MVC. Gateway authenticates API key / JWT, forwards `X-Merchant-Id` / `X-Key-Id`.
- **Monolith:** kept in `monolith/` through phases 0-6 as the reference behavior. Removed in phase 7; still on `master`.

## Target services

| Service | Port | Owns | Source in monolith |
|---|---|---|---|
| discovery-service | 8761 | Eureka registry | new |
| config-service | 8888 | central config | new |
| api-gateway-service | 8080 | routing, API key / JWT auth | `merchant/security/*Filter`, `JwtUtil` |
| common-lib | — | shared kernel (entities base, enums, exceptions, idempotency, rate limit, MerchantContext, shared DTOs) | `common/**`, `audit/**` |
| merchant-service | 8081 | merchants, users, API keys, customers, webhook config | `merchant/**` |
| vault-service | 8082 | card vault, tokens, card charge | `vault/**` |
| payment-service | 8083 | orders, payments, refunds, gateway adapters, state machine, outbox, bank simulator | `payment/**` |
| operations-service | 8084 | settlements, webhook delivery, DLQ, retry queue | `operations/**` |

## Phases (one commit each) - status

Status: all phases done on branch `microservices`. Verified against the local compose stack.

0. **Baseline** - done. Monolith moved to `monolith/`, plan added.
1. **Shared kernel + infra** - done. Aggregator pom, `common-lib`, `discovery-service`, `config-service`.
2. **merchant-service + gateway** - done. Merchant domain, internal lookups, gateway auth.
3. **vault-service** - done. Card vault, tokenize, internal charge.
4. **payment-service** - done. Orders, payments, refunds; Feign to merchant and vault.
5. **operations-service** - done. Settlement, webhooks, DLQ, Kafka consumers.
6. **Local infra** - done. Dockerfile, compose (postgres, redis, kafka, zipkin, prometheus, grafana, control center), Postgres init script.
7. **Cutover** - done. `monolith/` removed on this branch; still on `master`.
8. **Hardening (post-cutover)** - done: Flyway migrations (`V1` baseline, `V2` card_token customer), error mapping fixes, Grafana dashboards, Control Center.
9. **Tests** - done: unit (common-lib), Testcontainers integration (merchant, vault, payment, operations settlement and webhook), gateway auth filter tests, live smoke script (`scripts/smoke.sh`).
10. **Kubernetes** - done, verified on kind: kustomize manifests in `k8s/`, persistent volumes for Postgres, Redis, Kafka; monitoring (Prometheus, Grafana, Control Center) in `k8s/monitoring/`.
11. **Load test** - done: JMeter plan in `loadtest/`, key setup script.

Merged to `master` (fast-forward) and pushed to `origin`.

Not yet done: secret rotation (parked: GitHub PAT in the reference repo, JWT and vault keys on `master`, Grafana admin password). Production hardening beyond the local kind setup (TLS, ingress, managed Kafka and Postgres, HA replicas).

## Out of scope (for now)

- Ingress, TLS, managed cloud Kafka/Postgres/Redis, multi-replica HA.
- Automated settlement/webhook checks in CI (they run in the operations integration test; the smoke script is HTTP-only).

## Known issues to fix while migrating

- Reference `config-service` config commits a GitHub PAT. Do not copy it; use env vars.
- Monolith `application.yaml` has a hardcoded JWT secret and vault master key. Move to env vars.
- `ddl-auto: update` in monolith. Services use `validate` once schemas are stable (or keep `update` for dev only).

## Verification per phase

- `./mvnw -B -q -Djava.version=21 -DskipTests compile` (JDK 21 installed; project targets Java 25)
- Services that start: check `/actuator/health` once infra is up (phase 6).
