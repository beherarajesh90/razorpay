# Razorpay monolith → microservices migration

Branch: `microservices` (in this repo; `master` keeps the monolith).

## Decisions

- **Repo:** same repo, new branch. Keeps git history of the monolith; each phase is one commit.
- **Build:** Maven multi-module reactor at repo root. Parent manages Spring Boot 4.1.0 and Spring Cloud 2025.1.2 versions.
- **Packages:** keep `com.systemdesign.razorpay.*` so moved classes need few import edits.
- **Database:** one Postgres instance, one database per service (`merchant_db`, `payment_db`, `vault_db`, `operations_db`). No cross-service joins or shared tables.
- **Cross-service calls:** synchronous HTTP via OpenFeign (lookups, tokenize, charge). Async via Kafka outbox (payment/refund/settlement events).
- **Service discovery:** Eureka. **Config:** Spring Cloud Config, classpath/native profile (no secrets in git).
- **Edge:** Spring Cloud Gateway MVC. Gateway authenticates API key / JWT, forwards `X-Merchant-Id` / `X-Api-Key-Id`.
- **Monolith:** kept in `monolith/` until phase 8, as the reference behavior. Deleted at the end.

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

## Phases (one commit each)

0. **Baseline:** move monolith to `monolith/`, add this plan. Monolith still compiles.
1. **Shared kernel + infra:** root aggregator pom, `common-lib`, `discovery-service`, `config-service`.
2. **merchant-service + gateway:** merchant domain, internal lookup endpoints, gateway auth.
3. **vault-service:** vault domain, card charge moved here; internal tokenize/charge endpoints.
4. **payment-service:** payment/order/refund domain; Feign clients to merchant and vault.
5. **operations-service:** settlement, webhook, DLQ; Feign clients to merchant and payment; Kafka consumers.
6. **Local infra:** per-service Dockerfiles, compose file with one DB per service, Zipkin / Prometheus.
7. **Cutover:** delete `monolith/`, update README, run end-to-end smoke flow.

## Out of scope (for now)

- Kubernetes manifests (reference repo has them; add after phase 7).
- Load tests (JMeter).

## Known issues to fix while migrating

- Reference `config-service` config commits a GitHub PAT. Do not copy it; use env vars.
- Monolith `application.yaml` has a hardcoded JWT secret and vault master key. Move to env vars.
- `ddl-auto: update` in monolith. Services use `validate` once schemas are stable (or keep `update` for dev only).

## Verification per phase

- `./mvnw -B -q -Djava.version=21 -DskipTests compile` (JDK 21 installed; project targets Java 25)
- Services that start: check `/actuator/health` once infra is up (phase 6).
