CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE tenants (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE templates (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    format_string       VARCHAR(500) NOT NULL,
    max_counter_value   BIGINT      NOT NULL,
    counter_padding     INT         NOT NULL,
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_templates_tenant ON templates(tenant_id, is_active);

CREATE TABLE placeholder_configs (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    template_id         UUID        NOT NULL REFERENCES templates(id) ON DELETE CASCADE,
    placeholder_name    VARCHAR(50) NOT NULL,
    placeholder_type    VARCHAR(20) NOT NULL,
    date_format         VARCHAR(50),
    description         TEXT,
    is_required         BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order          INT         NOT NULL,
    UNIQUE (template_id, placeholder_name)
);

CREATE TABLE sequence_audit (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    template_id     UUID        NOT NULL REFERENCES templates(id),
    resolved_key    VARCHAR(500) NOT NULL,
    counter_value   BIGINT      NOT NULL,
    full_sequence   VARCHAR(500) NOT NULL,
    request_params  TEXT,
    generated_at    TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_tenant_template ON sequence_audit(tenant_id, template_id);
CREATE INDEX idx_audit_generated_at   ON sequence_audit(generated_at DESC);
CREATE INDEX idx_audit_full_sequence  ON sequence_audit(full_sequence);

-- Fallback counter table used when Redis is unavailable (Phase 4)
CREATE TABLE db_counters (
    resolved_key    VARCHAR(500) NOT NULL PRIMARY KEY,
    tenant_id       UUID        NOT NULL,
    template_id     UUID        NOT NULL,
    counter_value   BIGINT      NOT NULL DEFAULT 0,
    max_value       BIGINT      NOT NULL,
    updated_at      TIMESTAMP   NOT NULL DEFAULT now()
);

-- Seed a default tenant for local development
INSERT INTO tenants (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'default');
