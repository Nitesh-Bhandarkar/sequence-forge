<div align="center">

# ⚡ Sequence Forge

**Open-source multi-tenant service for generating unique, formatted sequence numbers at scale.**

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://react.dev)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io)

```
MH / IN / 2627 / 0042
 ↑     ↑    ↑     ↑
State Country FYear Counter
```

*Define a template once. Generate thousands of perfectly formatted, collision-free sequences per second.*

[Quick Start](#-quick-start) · [API Reference](#-api-reference) · [Deploy](#-deployment) · [Contributing](#-contributing)

</div>

---

## What is Sequence Forge?

Many business applications need structured, human-readable identifiers — invoice numbers, order IDs, case references, permit codes. Building this correctly is surprisingly hard: you need atomic counters, date-aware resets, multi-tenant isolation, and high throughput.

Sequence Forge solves this once, cleanly:

1. **Register a template** — define your format string and what each placeholder means
2. **Call the API** — pass your runtime values, get back a unique, formatted sequence
3. **Never worry about duplicates** — a Redis Lua script guarantees atomic, collision-free increments at 8 000+ RPS

---

## ✨ Features

| Feature | Details |
|---|---|
| **Flexible templates** | Any format: `{SS}/{CC}/{FY}/{SEQ}`, `INV-{YEAR}-{SEQ}`, `{DEPT}-{MONTH}-{SEQ}` |
| **Three placeholder types** | `STATIC` (caller-supplied), `DATE` (auto-resolved), `COUNTER` (atomic) |
| **Implicit counter reset** | New financial year → new Redis key → counter starts at 1 automatically |
| **Overflow protection** | Atomic Lua script detects overflow and returns HTTP 422 before incrementing |
| **Multi-tenancy** | Every template, counter, and audit record is scoped to a tenant |
| **Circuit breaker** | Resilience4j + PostgreSQL fallback counter when Redis is unavailable |
| **Template caching** | Redis-backed cache with 10-minute TTL — zero DB hits on the hot path |
| **Async audit log** | Every generated sequence is recorded to PostgreSQL asynchronously |
| **OAuth2 login** | Google and GitHub login out of the box |
| **API keys** | Generate and revoke API keys for programmatic access |
| **AI assistant** | Claude-powered placeholder classifier and template builder chat |
| **React dashboard** | Template builder, audit viewer, API key management |

---

## 🏗 Architecture

```
                         ┌─────────────────────────────────┐
                         │          React / TypeScript       │
                         │   Dashboard · Template Builder    │
                         │   Audit Log · AI Chat Panel       │
                         └──────────────┬──────────────────┘
                                        │ HTTPS / JWT
                         ┌──────────────▼──────────────────┐
                         │       Spring Boot 3.3.5           │
                         │       Java 21 Virtual Threads     │
                         │                                   │
                         │  TenantFilter → JwtFilter         │
                         │  TemplateService (cached)         │
                         │  SequenceGeneratorService         │
                         │  AiService (Claude API + SSE)     │
                         └──────┬──────────────┬────────────┘
                                │              │
               ┌────────────────▼──┐     ┌─────▼─────────────────┐
               │      Redis 7       │     │     PostgreSQL 16       │
               │  Lua atomic INCR   │     │  tenants               │
               │  Template cache    │◄────│  templates             │
               │  (Sentinel HA)     │     │  placeholder_configs   │
               └────────────────────┘     │  sequence_audit        │
                                          │  db_counters (fallback)│
                                          └────────────────────────┘
```

### Hot path — sequence generation

```
POST /api/v1/sequences/generate  (X-Api-Key)

  1. Validate API key  →  set TenantContext
  2. Load template     →  Redis cache (miss: DB + populate)
  3. Resolve STATIC    →  from request params
  4. Resolve DATE      →  financial year / year / month / ...
  5. Build Redis key   →  seq:{tenantId}:{templateId}:MH:IN:2627
  6. Lua INCR          →  atomic, overflow-guarded, single RTT
  7. Format counter    →  "%04d" → "0042"
  8. Build sequence    →  "MH/IN/2627/0042"
  9. Audit (async)     →  fire-and-forget to PostgreSQL
 10. Return response
```

---

## 📦 Quick Start

### Prerequisites

- **Docker** (for PostgreSQL + Redis)
- **Java 21+**
- **Node 18+** (for the frontend)
- **Maven 3.9+**

### 1 — Start infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL 16 on `:5432` and Redis 7 on `:6379`. Flyway runs the schema migration automatically on backend startup.

### 2 — Start the backend

```bash
cd backend
mvn spring-boot:run
```

The API is now available at `http://localhost:8080`.

> **First run:** Flyway creates all tables and seeds a default tenant (`00000000-0000-0000-0000-000000000001`).

### 3 — Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` — you'll see the login page.

### 4 — Log in (dev mode)

Dev login is enabled by default. Click **"Continue as dev user"** on the login page, or hit the API directly:

```bash
curl -s -X POST "http://localhost:8080/dev/login" | jq .
```

```json
{
  "token": "eyJ...",
  "tenantId": "81052dbd-..."
}
```

---

## 🔧 Core Concepts

### Placeholder types

| Type | Who provides the value | Example |
|---|---|---|
| `STATIC` | Caller passes it in `params` at request time | `"SS": "MH"` |
| `DATE` | Auto-resolved from the current date (or overridden in `params`) | `FY` → `2627` |
| `COUNTER` | Auto-incrementing atomic counter (exactly **one** per template) | `SEQ` → `0042` |

### DATE format codes

| Code | Output | Description |
|---|---|---|
| `FINANCIAL_YEAR` | `2627` | April–March year (May 2026 → 2026-27 → `2627`) |
| `FINANCIAL_YEAR_FULL` | `2026-27` | Full financial year label |
| `FINANCIAL_QUARTER` | `FQ1`–`FQ4` | Quarter within financial year |
| `YEAR_4` | `2026` | Calendar year |
| `YEAR_2` | `26` | 2-digit year |
| `MONTH_2` | `06` | Zero-padded month |
| `DAY_2` | `03` | Zero-padded day |
| `QUARTER` | `Q1`–`Q4` | Calendar quarter |
| `HALF_YEAR` | `H1` / `H2` | Half-year |
| `WEEK_OF_YEAR` | `01`–`53` | ISO week number |
| `YYYYMM` | `202606` | Compact year-month |
| `YYYYMMDD` | `20260603` | Compact date |

### Counter padding

Derived automatically from `maxCounterValue`:

| `maxCounterValue` | Padding | Example output |
|---|---|---|
| `9999` | 4 digits | `0042` |
| `99999` | 5 digits | `00042` |
| `999999` | 6 digits | `000042` |

### Implicit counter reset

No cron jobs needed. The Redis key embeds all resolved non-counter values:

```
seq:{tenantId}:{templateId}:MH:IN:2627   →  counter: 1, 2, 3 ...
seq:{tenantId}:{templateId}:MH:IN:2728   →  counter: 1, 2, 3 ...  ← new FY, new key
```

---

## 📡 API Reference

All endpoints require authentication. Template/management endpoints use a **JWT Bearer token** (from OAuth2 login). Sequence generation uses an **API key** header.

### Authentication

```bash
# Get a JWT (dev mode only)
curl -X POST http://localhost:8080/dev/login

# Create an API key (requires JWT)
curl -X POST http://localhost:8080/api/v1/apikeys \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"name": "production-app"}'
```

### Templates

```bash
# Create a template
curl -X POST http://localhost:8080/api/v1/templates \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Invoice",
    "formatString": "{SS}/{CC}/{FY}/{SEQ}",
    "maxCounterValue": 9999,
    "placeholders": [
      { "placeholderName": "SS",  "placeholderType": "STATIC",  "isRequired": true },
      { "placeholderName": "CC",  "placeholderType": "STATIC",  "isRequired": true },
      { "placeholderName": "FY",  "placeholderType": "DATE",    "dateFormat": "FINANCIAL_YEAR" },
      { "placeholderName": "SEQ", "placeholderType": "COUNTER" }
    ]
  }'

# List templates
curl http://localhost:8080/api/v1/templates \
  -H "Authorization: Bearer $JWT"

# Get single template
curl http://localhost:8080/api/v1/templates/{id} \
  -H "Authorization: Bearer $JWT"

# Delete (soft)
curl -X DELETE http://localhost:8080/api/v1/templates/{id} \
  -H "Authorization: Bearer $JWT"
```

### Generate a sequence

```bash
curl -X POST http://localhost:8080/api/v1/sequences/generate \
  -H "X-Api-Key: sf_..." \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "e0918051-...",
    "params": { "SS": "MH", "CC": "IN" }
  }'
```

```json
{
  "success": true,
  "data": {
    "sequence":     "MH/IN/2627/0042",
    "templateId":   "e0918051-...",
    "resolvedKey":  "seq:...:MH:IN:2627",
    "counterValue": 42,
    "generatedAt":  "2026-06-03T10:00:00"
  }
}
```

### Audit log

```bash
# Paginated audit log (most recent first)
curl "http://localhost:8080/api/v1/audit?page=0&size=20" \
  -H "Authorization: Bearer $JWT"
```

### Counter peek

```bash
# Check current counter value without incrementing
curl -X POST "http://localhost:8080/api/v1/sequences/counter?templateId={id}" \
  -H "X-Api-Key: sf_..." \
  -H "Content-Type: application/json" \
  -d '{"SS": "MH", "CC": "IN"}'
```

### AI assistant

```bash
# Classify a placeholder name
curl -X POST http://localhost:8080/api/v1/ai/classify-placeholder \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"placeholderName": "FY", "context": "Invoice number for Indian GST documents"}'

# Streaming template builder chat (SSE)
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"messages": [{"role": "user", "content": "Design a template for hospital patient admission IDs"}]}'
```

### Response envelope

All responses follow a consistent shape:

```json
{ "success": true,  "data":  { ... } }
{ "success": false, "error": "Human-readable message" }
```

### Error codes

| HTTP | Scenario |
|---|---|
| `400` | Validation failure, invalid template, missing placeholder value |
| `401` | Missing or invalid token / API key |
| `404` | Template not found |
| `422` | Counter overflow (`maxCounterValue` exceeded) |
| `503` | AI assistant disabled (`AI_ENABLED=false`) |
| `500` | Unexpected server error |

---

## ⚙️ Configuration

All settings live in `backend/src/main/resources/application.yml`. The table below covers the knobs you're most likely to turn.

| Environment variable | Default | Purpose |
|---|---|---|
| `ANTHROPIC_API_KEY` | *(empty)* | Enables AI assistant features |
| `AI_ENABLED` | `false` | Master toggle for AI endpoints |
| `APP_JWT_SECRET` | *(dev default)* | **Override in production** — `openssl rand -base64 32` |
| `APP_DEV_LOGIN_ENABLED` | `true` | Set `false` in production |
| `GOOGLE_CLIENT_ID` | `change-me` | Google OAuth2 credentials |
| `GOOGLE_CLIENT_SECRET` | `change-me` | Google OAuth2 credentials |
| `GITHUB_CLIENT_ID` | `change-me` | GitHub OAuth2 credentials |
| `GITHUB_CLIENT_SECRET` | `change-me` | GitHub OAuth2 credentials |
| `FRONTEND_URL` | `http://localhost:5173` | OAuth2 redirect target |
| `SENTINEL_NODES` | *(empty)* | Redis Sentinel nodes for HA, e.g. `sentinel1:26379` |

---

## 🗄 Database Schema

```
tenants             id · name · created_at
templates           id · tenant_id · name · format_string · max_counter_value · counter_padding · is_active
placeholder_configs id · template_id · placeholder_name · placeholder_type · date_format · sort_order
sequence_audit      id · tenant_id · template_id · resolved_key · counter_value · full_sequence · generated_at
db_counters         resolved_key (PK) · tenant_id · template_id · counter_value · max_value
```

Migrations are managed by **Flyway** and run automatically on startup. The only manual step is Docker (above).

---

## 🛡 Security

- **Multi-tenancy** — every DB query and cache key is scoped by `tenantId`; cross-tenant access is impossible at the query layer
- **JWT** — signed with HMAC-SHA256; configurable expiry (default 24 h)
- **API keys** — hashed with SHA-256 before storage; the plain key is shown only once
- **No secrets in code** — all sensitive values read from environment variables
- **Redis type safety** — Jackson deserialization restricted to `io.sequenceforge`, `java.util`, `java.time` packages
- **Atomic counters** — Redis Lua script is the single source of truth; no race conditions possible

---

## 🚀 Deployment

### Docker Compose (production-like)

```bash
APP_JWT_SECRET=$(openssl rand -base64 32) \
ANTHROPIC_API_KEY=sk-ant-... \
AI_ENABLED=true \
APP_DEV_LOGIN_ENABLED=false \
docker compose up -d
```

### Kubernetes — Helm

```bash
# 1. Create secrets
kubectl create secret generic sequence-forge-secrets \
  --from-literal=DB_PASSWORD=your_db_password \
  --from-literal=APP_JWT_SECRET=$(openssl rand -base64 32) \
  --from-literal=ANTHROPIC_API_KEY=sk-ant-... \
  --from-literal=GOOGLE_CLIENT_ID=... \
  --from-literal=GOOGLE_CLIENT_SECRET=...

# 2. Install
helm install sequence-forge ./helm/sequence-forge \
  --set database.url=jdbc:postgresql://postgres:5432/sequenceforge \
  --set redis.host=redis-master \
  --set app.frontendUrl=https://your-domain.com

# 3. Upgrade
helm upgrade sequence-forge ./helm/sequence-forge --reuse-values
```

The chart includes a **HorizontalPodAutoscaler** (2–10 replicas, 70% CPU target) and liveness/readiness probes on the Spring Boot Actuator health endpoints.

---

## 🔥 Load Testing

Sequence Forge targets **8 000 RPS** with p95 < 50 ms. Run the Gatling simulation:

```bash
# Prerequisites: Java, Maven
cd load-test

mvn gatling:test \
  -Dbase.url=http://localhost:8080 \
  -Dapi.key=sf_your_api_key \
  -Dtemplate.id=your-template-uuid
```

The simulation ramps from 1 → 8 000 RPS over 2 minutes, then holds for 5 minutes. Pass/fail assertions:

| Metric | Target |
|---|---|
| p95 latency | < 50 ms |
| p99 latency | < 200 ms |
| Success rate | > 99.9 % |

Reports are written to `load-test/target/gatling/`.

---

## 🏛 Project Structure

```
sequence-forge/
├── docker-compose.yml                   PostgreSQL 16 + Redis 7
├── LICENSE                              Apache 2.0
│
├── backend/                             Spring Boot application
│   └── src/main/java/io/sequenceforge/
│       ├── ai/                          Claude API integration + SSE chat
│       ├── apikey/                      API key CRUD + SHA-256 hashing
│       ├── audit/                       Async audit persistence
│       ├── auth/                        JWT + OAuth2 + API key filters
│       ├── common/                      TenantContext, ApiResponse, exceptions
│       ├── config/                      Redis cache, CORS, async executor
│       ├── counter/                     Resilience4j circuit breaker façade
│       ├── fallback/                    PostgreSQL fallback counter
│       ├── placeholder/                 Resolver plugin system (STATIC/DATE/COUNTER)
│       ├── redis/                       Lua script runner
│       ├── sequence/                    Generation engine + controller
│       ├── template/                    Template CRUD + validation
│       └── tenant/                      Tenant entity
│
├── frontend/                            React + TypeScript
│   └── src/
│       ├── api/                         Typed API clients
│       ├── components/                  Layout, AI chat panel, protected routes
│       └── pages/                       Dashboard, Templates, Builder, Audit, Keys
│
├── helm/sequence-forge/                 Kubernetes Helm chart
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/                       Deployment, Service, Ingress, HPA
│
└── load-test/                           Gatling simulation (8 000 RPS)
```

---

## 🔌 Extending — Add a new placeholder type

The resolver system is a Spring plugin registry. Adding a new type takes three steps:

**1. Add the enum value**

```java
// PlaceholderType.java
public enum PlaceholderType { STATIC, DATE, COUNTER, MY_NEW_TYPE }
```

**2. Implement the resolver**

```java
@Component
public class MyNewPlaceholderResolver implements PlaceholderResolver {

    @Override
    public PlaceholderType supportedType() { return PlaceholderType.MY_NEW_TYPE; }

    @Override
    public String resolve(PlaceholderConfig config, Map<String, String> params) {
        return /* your logic */;
    }
}
```

**3. Use it in a template**

```json
{ "placeholderName": "REF", "placeholderType": "MY_NEW_TYPE" }
```

Spring's `List<PlaceholderResolver>` injection auto-discovers it — no registration needed.

---

## 🧪 Running Tests

```bash
cd backend
mvn test
```

The test suite covers:
- `DatePlaceholderResolverTest` — financial year, quarter, and date format edge cases
- `TemplateServiceTest` — validation, placeholder counting, format string parsing
- `SequenceGeneratorServiceTest` — full generation pipeline with mocked Redis
- `DbCounterServiceTest` — fallback counter increment and overflow
- `JwtServiceTest` / `ApiKeyServiceTest` — auth flows

---

## 🤝 Contributing

Contributions are welcome! Please:

1. **Fork** the repository
2. **Create a branch** — `git checkout -b feat/your-feature`
3. **Make your changes** and add tests
4. **Run the suite** — `mvn test`
5. **Open a pull request** with a clear description

For significant changes, open an issue first to discuss the approach.

### Build phases

| Phase | Status | Scope |
|---|---|---|
| 1 — Core backend | ✅ Done | Template CRUD, STATIC+DATE resolvers, Redis Lua counter, async audit |
| 2 — Auth | ✅ Done | OAuth2 (Google/GitHub), JWT, API key management |
| 3 — DATE enhancements | ✅ Done | Extended date formats, audit endpoint, counter peek, template update |
| 4 — Reliability | ✅ Done | Redis Sentinel HA, Resilience4j circuit breaker, PostgreSQL fallback, template caching |
| 5 — Frontend | ✅ Done | React/TS: template builder, dashboard, audit viewer, API key management |
| 6 — AI integration | ✅ Done | Claude API: placeholder classifier, SSE template builder chat |
| 7 — Open source polish | ✅ Done | Gatling load tests, Kubernetes Helm chart, Apache 2.0 LICENSE |

---

## 📄 License

Sequence Forge is licensed under the **Apache License 2.0** — see [LICENSE](LICENSE) for the full text.

---

<div align="center">

Built with ☕ Java, ⚛️ React, and a lot of ❤️ for clean APIs.

**[⭐ Star us on GitHub](https://github.com/Nitesh-Bhandarkar/sequence-forge)** if Sequence Forge saves you time!

</div>
