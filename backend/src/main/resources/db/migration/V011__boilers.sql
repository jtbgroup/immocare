-- ============================================================
-- V011 — UC011: Boilers
-- ============================================================

-- ─── boiler ──────────────────────────────────────────────────────────────────

CREATE TABLE boiler (
    id                BIGSERIAL    PRIMARY KEY,
    owner_type        VARCHAR(20)  NOT NULL CHECK (owner_type IN ('HOUSING_UNIT','BUILDING')),
    owner_id          BIGINT       NOT NULL,
    brand             VARCHAR(100) NULL,
    model             VARCHAR(100) NULL,
    serial_number     VARCHAR(100) NULL,
    fuel_type         VARCHAR(20)  NOT NULL CHECK (fuel_type IN ('GAS','OIL','ELECTRIC','HEAT_PUMP')),
    installation_date DATE         NOT NULL,
    removal_date      DATE         NULL,
    last_service_date DATE         NULL,
    next_service_date DATE         NULL,
    notes             TEXT         NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by        BIGINT       REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_boiler_owner        ON boiler (owner_type, owner_id);
CREATE INDEX idx_boiler_active       ON boiler (owner_type, owner_id) WHERE removal_date IS NULL;

-- ─── boiler_service ──────────────────────────────────────────────────────────

CREATE TABLE boiler_service (
    id           BIGSERIAL PRIMARY KEY,
    boiler_id    BIGINT    NOT NULL REFERENCES boiler (id) ON DELETE RESTRICT,
    service_date DATE      NOT NULL,
    valid_until  DATE      NOT NULL,
    notes        TEXT      NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by   BIGINT    REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_boiler_service_boiler ON boiler_service (boiler_id);
CREATE INDEX idx_boiler_service_date   ON boiler_service (boiler_id, service_date DESC);