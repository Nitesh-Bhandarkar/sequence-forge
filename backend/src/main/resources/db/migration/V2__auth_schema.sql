CREATE TABLE users (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(255),
    oauth_provider  VARCHAR(50) NOT NULL,
    oauth_subject   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    UNIQUE (oauth_provider, oauth_subject)
);

CREATE INDEX idx_users_tenant ON users(tenant_id);

CREATE TABLE api_keys (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id       UUID        NOT NULL REFERENCES tenants(id),
    key_hash        VARCHAR(64) NOT NULL UNIQUE,
    key_prefix      VARCHAR(10) NOT NULL,
    name            VARCHAR(255),
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),
    last_used_at    TIMESTAMP
);

CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id, is_active);
