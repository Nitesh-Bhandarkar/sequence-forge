# Sequence Forge — Project Guide

Open source multi-tenant service that generates unique, formatted sequence numbers from configurable templates (e.g. `{SS}/{CC}/{FY}/{SEQ}` → `MH/IN/2627/0042`).

All 7 build phases are complete. The project is production-ready and open-sourced under Apache 2.0.

---

## Quick Start (local dev)

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Run backend
cd backend && mvn spring-boot:run

# 3. Run frontend (separate terminal)
cd frontend && npm install && npm run dev

# 4. Open http://localhost:5173 — click "Continue as dev user"
#    OR get a JWT directly:
curl -X POST http://localhost:8080/dev/login
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 (virtual threads), Spring Boot 3.3.5 |
| Web | Spring MVC (synchronous; virtual threads handle concurrency) |
| Persistence | PostgreSQL 16 + Flyway migrations |
| Counter store | Redis 7 (Lua script for atomic increment) |
| Cache | Redis (GenericJackson2JsonRedisSerializer, 10-min TTL) |
| Reliability | Resilience4j circuit breaker → PostgreSQL fallback counter |
| Auth | OAuth2 (Google + GitHub) + JWT + API key (SHA-256 hashed) |
| Build | Maven 3.9 — `mvn test` / `mvn spring-boot:run` |
| Frontend | React 18 + TypeScript + Vite + TanStack Query |
| AI | Claude API (Anthropic SDK 2.34.0) — SSE streaming chat + classifier |
| Deployment | Docker Compose (dev) + Kubernetes Helm chart (prod) |
| Load test | Gatling 3.11.5 — targets 8 000 RPS |

> **Java version note:** System runs Java 25. Requires `-Dnet.bytebuddy.experimental=true` in Surefire config (already set in pom.xml) for Mockito to work.

---

## Project Layout

```
sequence-forge/
├── LICENSE                              Apache 2.0
├── README.md
├── CLAUDE.md                            ← this file
├── docker-compose.yml                   PostgreSQL 16 + Redis 7
│
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/io/sequenceforge/
│       │   ├── SequenceForgeApplication.java
│       │   ├── ai/                      Claude API: classify-placeholder + SSE chat
│       │   │   ├── AiConfig.java        AnthropicClient bean (reads ANTHROPIC_API_KEY)
│       │   │   ├── AiController.java    POST /api/v1/ai/classify-placeholder + /chat
│       │   │   ├── AiService.java       classifyPlaceholder() + streamChat()
│       │   │   └── dto/                 ChatRequest, ClassifyPlaceholderRequest/Response
│       │   ├── apikey/                  API key CRUD (SHA-256 hashed storage)
│       │   ├── audit/                   Async audit write (@Async "auditExecutor")
│       │   ├── auth/
│       │   │   ├── SecurityConfig.java  Two filter chains: API key (order 1) + JWT (order 2)
│       │   │   ├── ApiKeyAuthFilter.java X-Api-Key header → TenantContext
│       │   │   ├── JwtAuthFilter.java   Bearer token → TenantContext + SecurityContext
│       │   │   ├── JwtService.java      HMAC-SHA256 sign/verify
│       │   │   ├── OAuth2SuccessHandler.java  Redirects to frontend with JWT query param
│       │   │   └── DevLoginController.java    POST /dev/login (disabled in prod)
│       │   ├── common/
│       │   │   ├── ApiResponse.java     {success, data, error} response envelope
│       │   │   ├── TenantContext.java   ThreadLocal<UUID> — set/get/clear
│       │   │   ├── TenantFilter.java    Legacy X-Tenant-ID header filter (still active)
│       │   │   └── exception/          Typed exceptions + GlobalExceptionHandler
│       │   ├── config/
│       │   │   ├── AsyncConfig.java     "auditExecutor" pool (4–8 threads, 10k queue)
│       │   │   ├── CacheConfig.java     RedisCacheManager — dedicated ObjectMapper with
│       │   │   │                         NON_FINAL typing + JavaTime (no Hibernate proxies)
│       │   │   ├── CorsConfig.java      CORS — allows localhost:5173 in dev
│       │   │   └── RedisConfig.java     RedisTemplate<String,String> + Lua script bean
│       │   ├── counter/
│       │   │   └── CounterService.java  Resilience4j @CircuitBreaker façade over Redis
│       │   ├── fallback/
│       │   │   ├── DbCounter.java       JPA entity (resolved_key PK)
│       │   │   ├── DbCounterRepository.java  findByResolvedKeyWithLock + insertIfAbsent
│       │   │   └── DbCounterService.java     INSERT…ON CONFLICT DO NOTHING + lock-read
│       │   ├── placeholder/             Resolver plugin system
│       │   │   ├── PlaceholderResolver.java   Interface: supportedType() + resolve()
│       │   │   ├── ResolverRegistry.java      Auto-discovers all @Component resolvers
│       │   │   ├── StaticPlaceholderResolver.java
│       │   │   └── DatePlaceholderResolver.java  12 date format codes (incl. financial year)
│       │   ├── redis/
│       │   │   └── LuaScriptRunner.java  incrementAndGet() + getCurrentValue()
│       │   ├── sequence/
│       │   │   ├── SequenceController.java   POST /generate + POST /counter
│       │   │   ├── SequenceGeneratorService.java  10-step generation pipeline
│       │   │   └── dto/
│       │   ├── template/               Template + PlaceholderConfig JPA entities, CRUD
│       │   ├── tenant/                 Tenant entity + repository
│       │   └── user/                   User entity + repository (OAuth subject + tenantId)
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── db/migration/V1__initial_schema.sql
│       │   └── scripts/increment_counter.lua
│       └── test/java/io/sequenceforge/
│           ├── apikey/ApiKeyServiceTest.java
│           ├── auth/JwtServiceTest.java
│           ├── fallback/DbCounterServiceTest.java
│           ├── placeholder/DatePlaceholderResolverTest.java
│           ├── sequence/SequenceGeneratorServiceTest.java
│           └── template/TemplateServiceTest.java
│
├── frontend/
│   └── src/
│       ├── api/                         Typed fetch clients (templates, sequences, ai, …)
│       ├── components/
│       │   ├── AIChatPanel.tsx          SSE streaming chat (fetch + ReadableStream)
│       │   ├── Layout.tsx
│       │   └── ProtectedRoute.tsx
│       └── pages/
│           ├── LoginPage.tsx            Google/GitHub OAuth + dev login
│           ├── DashboardPage.tsx        Stats + recent sequences
│           ├── TemplatesPage.tsx        Template list
│           ├── TemplateBuilderPage.tsx  Create template + AI classifier + chat panel
│           ├── AuditPage.tsx            Paginated audit log
│           └── ApiKeysPage.tsx          Generate / revoke API keys
│
├── helm/sequence-forge/                 Kubernetes Helm chart
│   ├── Chart.yaml
│   ├── values.yaml                      Replicas, HPA, ingress, resource limits
│   └── templates/
│       ├── _helpers.tpl
│       ├── deployment.yaml              All env vars from secret + values
│       ├── service.yaml
│       ├── ingress.yaml
│       └── hpa.yaml                     2–10 replicas, 70% CPU target
│
└── load-test/                           Gatling simulation
    ├── pom.xml
    └── src/test/scala/.../SequenceGenerationSimulation.scala
        # Ramp 1→8000 RPS over 2 min, hold 5 min
        # Assertions: p95<50ms, p99<200ms, 99.9% success
```

---

## Authentication — Two Filter Chains

**Chain 1** (order 1) — sequence generation + counter peek
- Matcher: `/api/v1/sequences/generate`, `/api/v1/sequences/counter`
- Auth: `X-Api-Key` header → `ApiKeyAuthFilter` → validates hash in DB → sets `TenantContext`
- Stateless

**Chain 2** (order 2) — everything else (templates, audit, API key CRUD, AI)
- Auth: `Authorization: Bearer <jwt>` → `JwtAuthFilter` → sets `TenantContext` + `SecurityContext`
- OAuth2 login flows: `/oauth2/**`, `/login/**`
- Public: `/dev/**`, `/error`, `/actuator/**`

### Dev login (local only)
```bash
curl -X POST http://localhost:8080/dev/login?email=dev@sequenceforge.io
# Returns: { "token": "eyJ...", "tenantId": "..." }
```
Controlled by `app.dev-login.enabled` (default `true`; set `false` in prod).

---

## Core Concepts

### Placeholder Types

| Type | Behaviour |
|---|---|
| `STATIC` | Caller provides value in `params` at request time |
| `DATE` | Resolved from `params` if present (backdating), else from current date via `dateFormat` |
| `COUNTER` | Atomic auto-incrementing counter in Redis. Exactly **one** per template. |

### DATE Format Codes

| Code | Example output | Notes |
|---|---|---|
| `FINANCIAL_YEAR` | `2627` | April–March. May 2026 → `2627` |
| `FINANCIAL_YEAR_FULL` | `2026-27` | Full label |
| `FINANCIAL_QUARTER` | `FQ1`–`FQ4` | Within financial year |
| `YEAR_4` | `2026` | Calendar year |
| `YEAR_2` | `26` | 2-digit year |
| `MONTH_2` | `06` | Zero-padded |
| `DAY_2` | `05` | Zero-padded |
| `QUARTER` | `Q1`–`Q4` | Calendar quarter |
| `HALF_YEAR` | `H1` / `H2` | |
| `WEEK_OF_YEAR` | `01`–`53` | ISO week |
| `YYYYMM` | `202606` | Compact year-month |
| `YYYYMMDD` | `20260605` | Compact date |

### Counter Padding

Derived from `maxCounterValue` at template registration:
- `maxCounterValue = 9999` → padding = 4 → `0042`
- `maxCounterValue = 999999` → padding = 6 → `000042`

### Implicit Counter Reset

No cron jobs. Redis key embeds all resolved non-COUNTER values:
```
seq:{tenantId}:{templateId}:MH:IN:2627   → counter 1,2,3...
seq:{tenantId}:{templateId}:MH:IN:2728   → counter 1,2,3...  (new FY = new key)
```

---

## Sequence Generation — Critical Path

```
POST /api/v1/sequences/generate
  X-Api-Key: sf_...
  { "templateId": "uuid", "params": { "SS": "MH", "CC": "IN" } }

1. ApiKeyAuthFilter  → validate key hash → TenantContext.set(tenantId)
2. loadTemplate      → Redis cache (miss: DB via findByIdAndTenantIdAndIsActiveTrue)
3. resolveStatic     → StaticPlaceholderResolver reads params
4. resolveDate       → DatePlaceholderResolver: params override or computed
5. buildRedisKey     → "seq:{tenantId}:{templateId}:MH:IN:2627"
6. luaINCR           → atomic, overflow-guarded, single Redis RTT
                        (fallback: DbCounterService with INSERT ON CONFLICT + SELECT FOR UPDATE)
7. formatCounter     → String.format("%04d", 42) → "0042"
8. buildSequence     → regex replace {SS}→MH, {CC}→IN, {FY}→2627, {SEQ}→0042
9. auditService      → @Async fire-and-forget to PostgreSQL
10. return           → GenerateSequenceResponse
```

Target throughput: **8 000 RPS**. Zero DB on hot path when Redis + cache are warm.

---

## API Reference

### Template Management (JWT required)
```
POST   /api/v1/templates          Create template
GET    /api/v1/templates          List active templates for tenant
GET    /api/v1/templates/{id}     Get template
PATCH  /api/v1/templates/{id}     Update name/description
DELETE /api/v1/templates/{id}     Soft-delete
```

### Sequence Generation (API key required)
```
POST   /api/v1/sequences/generate               Generate sequence
POST   /api/v1/sequences/counter?templateId={}  Peek current counter (no increment)
```

### Audit (JWT required)
```
GET    /api/v1/audit?page=0&size=20   Paginated audit log (most recent first)
```

### API Keys (JWT required)
```
POST   /api/v1/apikeys          Create API key (returns plain key once)
GET    /api/v1/apikeys          List keys (prefix + metadata, never plain key)
DELETE /api/v1/apikeys/{id}     Revoke key
```

### AI Assistant (JWT required)
```
POST   /api/v1/ai/classify-placeholder    Classify placeholder name → type + dateFormat
POST   /api/v1/ai/chat                    SSE streaming template builder chat
```
Controlled by `app.ai.enabled` / `AI_ENABLED` env var. Returns HTTP 503 when disabled.

---

## Database Schema

```sql
tenants             (id UUID PK, name, created_at)
users               (id UUID PK, tenant_id FK, email, name, oauth_provider, oauth_subject)
api_keys            (id UUID PK, tenant_id FK, name, key_hash, key_prefix, created_at, is_active)
templates           (id UUID PK, tenant_id FK, name, description, format_string,
                     max_counter_value, counter_padding, is_active, created_at, updated_at)
placeholder_configs (id UUID PK, template_id FK, placeholder_name, placeholder_type,
                     date_format, description, is_required, sort_order)
sequence_audit      (id UUID PK, tenant_id, template_id, resolved_key, counter_value,
                     full_sequence, request_params TEXT, generated_at)
db_counters         (resolved_key PK, tenant_id, template_id, counter_value, max_value, updated_at)
```

Migration: `backend/src/main/resources/db/migration/V1__initial_schema.sql`
Seeds default tenant: `00000000-0000-0000-0000-000000000001`

---

## Redis Cache — Important Notes

- `TemplateService.loadForGeneration()` is `@Cacheable(value="templates", key="tenantId:templateId")`
- Cache key **must** include tenantId to prevent cross-tenant cache poisoning
- The `CacheConfig` uses a dedicated `ObjectMapper` (not the app-wide one) with:
  - `JavaTimeModule` — for `LocalDateTime` serialization
  - `NON_FINAL` default typing — so Redis stores `@class` for correct deserialization
  - Type validator restricted to `io.sequenceforge`, `java.util`, `java.time` (security)
- Before caching, `PersistentBag` is copied to `ArrayList` so deserialization works without a Hibernate session
- Cache TTL: 10 minutes. Evicted on template update/delete (`@CacheEvict`)

---

## Resilience4j Circuit Breaker

```yaml
# application.yml — redis-counter circuit breaker
slidingWindowSize: 10
minimumNumberOfCalls: 5
failureRateThreshold: 50      # open at 50% failures
waitDurationInOpenState: 30s
slowCallDurationThreshold: 2s
slowCallRateThreshold: 80
```

When open: `CounterService` falls back to `DbCounterService`:
- `insertIfAbsent()` — `INSERT … ON CONFLICT DO NOTHING` (race-safe first insert)
- `findByResolvedKeyWithLock()` — `SELECT … FOR UPDATE` (serialized increment)

---

## Lua Script

`backend/src/main/resources/scripts/increment_counter.lua`

Single Redis round-trip: atomically reads current value, checks overflow, increments.
If `current >= max_val` → decrements back → returns `"SEQUENCE_OVERFLOW"` string error.
`LuaScriptRunner` converts this to `CounterOverflowException` → HTTP 422.

---

## AI Integration

**Placeholder classifier** — `POST /api/v1/ai/classify-placeholder`
- Sends placeholder name + optional context to Claude
- Returns `{ placeholderType, dateFormat, description, reasoning }`
- Uses prompt caching (`CacheControlEphemeral`) on the system prompt

**Template builder chat** — `POST /api/v1/ai/chat`
- Streaming SSE: virtual thread calls Claude streaming API, events forwarded via `SseEmitter`
- Uses extended thinking (`ThinkingConfigAdaptive`) for better template design reasoning
- Frontend reads via `fetch` + `ReadableStream`

Model: `claude-opus-4-8`. Toggle: `app.ai.enabled` / `AI_ENABLED` env var (default `false`).

---

## Resolver Plugin System

To add a new placeholder type:
1. Add value to `PlaceholderType` enum
2. Create `@Component` implementing `PlaceholderResolver` — implement `supportedType()` + `resolve()`
3. `ResolverRegistry` auto-discovers it via Spring's `List<PlaceholderResolver>` injection

---

## Error Responses

All errors use `ApiResponse.error(message)` envelope:

| Exception | HTTP |
|---|---|
| `TemplateNotFoundException` | 404 |
| `PlaceholderValueMissingException` | 400 |
| `InvalidTemplateException` | 400 |
| `CounterOverflowException` | 422 |
| `AiDisabledException` | 503 |
| `ApiKeyNotFoundException` | 404 |
| Validation (`@Valid`) | 400 |
| Unexpected | 500 |

---

## Configuration Reference

| Env var | Default | Purpose |
|---|---|---|
| `ANTHROPIC_API_KEY` | *(empty)* | Enables AI features |
| `AI_ENABLED` | `false` | Toggle AI endpoints (503 when false) |
| `APP_JWT_SECRET` | *(dev default)* | Override in prod — `openssl rand -base64 32` |
| `APP_DEV_LOGIN_ENABLED` | `true` | Set `false` in prod |
| `GOOGLE_CLIENT_ID` | `change-me` | Google OAuth2 |
| `GOOGLE_CLIENT_SECRET` | `change-me` | Google OAuth2 |
| `GITHUB_CLIENT_ID` | `change-me` | GitHub OAuth2 |
| `GITHUB_CLIENT_SECRET` | `change-me` | GitHub OAuth2 |
| `FRONTEND_URL` | `http://localhost:5173` | OAuth2 redirect base URL |
| `SENTINEL_NODES` | *(empty)* | Redis Sentinel e.g. `sentinel1:26379,sentinel2:26379` |

---

## Build Phases — All Complete

| Phase | Status | Scope |
|---|---|---|
| **1 — Core backend** | ✅ Done | Template CRUD, STATIC+DATE resolvers, Redis Lua counter, async audit |
| **2 — Auth** | ✅ Done | OAuth2 (Google/GitHub), JWT, API key generation + SHA-256 hashing |
| **3 — DATE enhancements** | ✅ Done | 12 date formats, audit endpoint, counter peek, template update |
| **4 — Reliability** | ✅ Done | Redis Sentinel HA, Resilience4j circuit breaker, PG fallback, template caching |
| **5 — Frontend** | ✅ Done | React/TS: template builder, dashboard, audit viewer, API key management |
| **6 — AI integration** | ✅ Done | Claude API: placeholder classifier, SSE streaming template builder chat |
| **7 — Open source polish** | ✅ Done | Gatling load tests (8000 RPS), Kubernetes Helm chart, Apache 2.0 LICENSE |

---

## Security Notes (for future contributors)

- Cache key for `loadForGeneration` **must** include tenantId — removing it enables cross-tenant cache poisoning
- `loadForGeneration` uses `findByIdAndTenantIdAndIsActiveTrue` — never use bare `findById` for tenant-owned resources
- Redis Jackson type validator is intentionally restricted to 3 packages — do not expand to `Object.class`
- `DbCounterService.insertIfAbsent` uses native SQL `ON CONFLICT DO NOTHING` — this is deliberate for race safety; do not replace with an `orElseGet` pattern
- `app.dev-login.enabled` must be `false` in any internet-facing deployment
- JWT secret has a dev default in source — always override via `APP_JWT_SECRET` env var in prod
