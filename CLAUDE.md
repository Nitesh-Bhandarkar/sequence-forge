# Sequence Forge — Project Guide

Open source multi-tenant service that generates unique, formatted sequence numbers from configurable templates (e.g. `{SS}/{CC}/{FY}/{NNNN}` → `MH/IN/2627/0042`).

## Quick Start (local dev)

```bash
# 1. Start infrastructure
docker compose up -d

# 2. Run backend
cd backend && mvn spring-boot:run

# 3. All requests require X-Tenant-ID header (UUID)
#    Default dev tenant: 00000000-0000-0000-0000-000000000001
```

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 (virtual threads), Spring Boot 3.3.5 |
| Web | Spring MVC (synchronous; virtual threads handle concurrency) |
| Persistence | PostgreSQL 16 + Flyway migrations |
| Counter store | Redis 7 (Lua script for atomic increment) |
| Build | Maven 3.9, `mvn test` / `mvn spring-boot:run` |
| Frontend (Phase 5) | React + TypeScript |
| AI (Phase 6) | Claude API (Anthropic) |

> **Java version note:** System runs Java 25. Requires `-Dnet.bytebuddy.experimental=true` in Surefire config (already set in pom.xml) for Mockito to work.

## Project Layout

```
sequence-generator/
├── CLAUDE.md                          ← this file
├── docker-compose.yml                 ← PostgreSQL 16 + Redis 7
└── backend/
    ├── pom.xml
    └── src/
        ├── main/java/io/sequenceforge/
        │   ├── SequenceForgeApplication.java
        │   ├── config/
        │   │   ├── RedisConfig.java       ← RedisTemplate<String,String> + Lua script bean
        │   │   └── AsyncConfig.java       ← "auditExecutor" thread pool (4–8 threads, 10k queue)
        │   ├── common/
        │   │   ├── ApiResponse.java       ← {success, data, error} wrapper
        │   │   ├── TenantContext.java      ← ThreadLocal<UUID> for current tenant
        │   │   ├── TenantFilter.java       ← reads X-Tenant-ID header → TenantContext
        │   │   └── exception/             ← typed exceptions + GlobalExceptionHandler
        │   ├── tenant/                    ← Tenant entity + repository
        │   ├── template/                  ← Template + PlaceholderConfig entities, CRUD
        │   ├── placeholder/               ← Resolver plugin system
        │   ├── redis/                     ← LuaScriptRunner
        │   ├── sequence/                  ← Generation engine + controller
        │   └── audit/                     ← Async audit persistence
        ├── main/resources/
        │   ├── application.yml
        │   ├── db/migration/V1__initial_schema.sql
        │   └── scripts/increment_counter.lua
        └── test/java/io/sequenceforge/
            ├── placeholder/DatePlaceholderResolverTest.java
            ├── template/TemplateServiceTest.java
            └── sequence/SequenceGeneratorServiceTest.java
```

## Core Concepts

### Placeholder Types

Every placeholder in a template format string is custom-typed:

| Type | Behaviour |
|---|---|
| `STATIC` | Value must be provided by caller in `params` at request time |
| `DATE` | Resolved from `params` if present (supports backdating), else computed from current date using `dateFormat` |
| `COUNTER` | Auto-incrementing atomic counter stored in Redis. Exactly **one** COUNTER per template. |

**DATE format codes:** `FINANCIAL_YEAR` (→ `2627`), `YEAR_4`, `YEAR_2`, `MONTH_2`, `DAY_2`, `YYYYMM`, `YYYYMMDD`

Financial year rule: April–March. May 2026 → FY 2026-27 → `2627`.

### Counter Padding

Derived at template registration from `maxCounterValue`:
- `maxCounterValue = 9999` → `counterPadding = 4` → counter formatted as `0042`
- `maxCounterValue = 999999` → `counterPadding = 6` → counter formatted as `000042`

### Implicit Counter Reset

Counter reset requires **no cron jobs**. The Redis key includes all resolved non-COUNTER values in `sort_order`. A new financial year produces a new key → counter starts at 1 automatically.

```
seq:{tenantId}:{templateId}:{SS}:{CC}:{FY}
seq:acme:inv-001:MH:IN:2627   → counter 1,2,3...
seq:acme:inv-001:MH:IN:2728   → counter 1,2,3...  (new FY = new key)
```

### Overflow Handling

When counter exceeds `maxCounterValue`, the Lua script atomically decrements back and returns `SEQUENCE_OVERFLOW`. `LuaScriptRunner` converts this to `CounterOverflowException` → HTTP 422.

## Sequence Generation — Critical Path

```
POST /api/v1/sequences/generate
  X-Tenant-ID: {uuid}
  { "templateId": "...", "params": { "SS": "MH", "CC": "IN" } }

1. TenantFilter   → parse X-Tenant-ID → TenantContext.set()
2. loadTemplate   → from DB (cache in Phase 4)
3. resolveStatic  → StaticPlaceholderResolver reads params
4. resolveDate    → DatePlaceholderResolver: params override or computed
5. buildRedisKey  → "seq:{tenantId}:{templateId}:MH:IN:2627"
6. luaINCR        → atomic, guards overflow
7. formatCounter  → String.format("%04d", 42) → "0042"
8. buildSequence  → regex replace {SS}→MH, {CC}→IN, {FY}→2627, {SEQ}→0042
9. auditService.record() → @Async fire-and-forget to PostgreSQL
10. return GenerateSequenceResponse
```

Target throughput: **8000 RPS**. Virtual threads + Redis Lua = no DB on hot path.

## API Reference

### Headers
All endpoints require `X-Tenant-ID: <uuid>`. Phase 2 adds OAuth + API key auth.

### Template Management
```
POST   /api/v1/templates        Create template
GET    /api/v1/templates        List active templates for tenant
GET    /api/v1/templates/{id}   Get template by ID
DELETE /api/v1/templates/{id}   Soft-delete template
```

**Create template request:**
```json
{
  "name": "Invoice",
  "description": "Invoice number template",
  "formatString": "{SS}/{CC}/{FY}/{SEQ}",
  "maxCounterValue": 9999,
  "placeholders": [
    { "placeholderName": "SS", "placeholderType": "STATIC", "description": "State code", "isRequired": true },
    { "placeholderName": "CC", "placeholderType": "STATIC", "description": "Country code", "isRequired": true },
    { "placeholderName": "FY", "placeholderType": "DATE", "dateFormat": "FINANCIAL_YEAR", "description": "Financial year", "isRequired": true },
    { "placeholderName": "SEQ", "placeholderType": "COUNTER", "description": "Sequence number", "isRequired": true }
  ]
}
```

### Sequence Generation
```
POST /api/v1/sequences/generate

{
  "templateId": "uuid",
  "params": { "SS": "MH", "CC": "IN" }
}

Response:
{
  "success": true,
  "data": {
    "sequence": "MH/IN/2627/0042",
    "templateId": "uuid",
    "resolvedKey": "seq:...:MH:IN:2627",
    "counterValue": 42,
    "generatedAt": "2026-05-31T10:00:00"
  }
}
```

## Database Schema

```sql
tenants             (id UUID PK, name, created_at)
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

## Lua Script

`backend/src/main/resources/scripts/increment_counter.lua`

Atomically increments the Redis counter. If `current > max_val`, decrements back and returns error `SEQUENCE_OVERFLOW`. The entire check-and-increment is a single Redis round-trip with no race conditions.

## Resolver Plugin System

To add a new placeholder type:
1. Add value to `PlaceholderType` enum
2. Create `@Component` implementing `PlaceholderResolver` with `supportedType()` returning the new type
3. `ResolverRegistry` auto-discovers it via Spring's `List<PlaceholderResolver>` injection

## Multi-Tenancy

- All DB queries are scoped by `tenantId` from `TenantContext` (ThreadLocal)
- `TenantFilter` (`OncePerRequestFilter`) populates and clears the context per request
- Phase 2 replaces the header-based approach with OAuth + API key validation

## Error Responses

| Exception | HTTP |
|---|---|
| `TemplateNotFoundException` | 404 |
| `PlaceholderValueMissingException` | 400 |
| `InvalidTemplateException` | 400 |
| `CounterOverflowException` | 422 |
| Validation (`@Valid`) | 400 |
| Unexpected | 500 |

## Build Phases

| Phase | Status | Scope |
|---|---|---|
| **1 — Core backend** | ✅ Done | Template CRUD, STATIC+DATE resolvers, Redis Lua counter, async audit |
| **2 — Auth** | Planned | OAuth2 (Google/GitHub), tenant validation, API key generation + middleware |
| **3 — DATE enhancements** | Planned | Additional date formats, validation improvements |
| **4 — Reliability** | Planned | Redis Sentinel HA, Resilience4j circuit breaker, PG fallback counter, template caching |
| **5 — Frontend** | Planned | React/TS app: template builder, dashboard, audit viewer |
| **6 — AI integration** | Planned | Claude API: placeholder classifier, template builder chat panel |
| **7 — Open source polish** | Planned | Load tests (Gatling), Kubernetes Helm chart, Apache 2.0 LICENSE |

## Configuration

Key `application.yml` settings:
```yaml
spring:
  datasource.url: jdbc:postgresql://localhost:5432/sequenceforge
  data.redis.host: localhost
  data.redis.lettuce.pool.max-active: 50
  threads.virtual.enabled: true   # Java 21 virtual threads
```
