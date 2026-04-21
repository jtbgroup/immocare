-- ============================================================
-- V010 — UC010: Manage Boilers
-- Tables: boiler, boiler_service, boiler_service_validity_rule
-- Note: boiler_service_validity_rule is part of this UC as it
--       directly governs the validity calculation of boiler services.
-- ============================================================

CREATE TABLE boiler (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_type        VARCHAR(20)  NOT NULL,
    owner_id          BIGINT       NOT NULL,
    brand             VARCHAR(100) NULL,
    model             VARCHAR(100) NULL,
    serial_number     VARCHAR(100) NULL,
    fuel_type         VARCHAR(20)  NOT NULL,
    installation_date DATE         NOT NULL,
    removal_date      DATE         NULL,
    last_service_date DATE         NULL,
    next_service_date DATE         NULL,
    notes             TEXT         NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by        BIGINT       REFERENCES app_user (id) ON DELETE SET NULL,

    CONSTRAINT chk_boiler_owner_type CHECK (owner_type IN ('HOUSING_UNIT','BUILDING')),
    CONSTRAINT chk_boiler_fuel_type  CHECK (fuel_type  IN ('GAS','OIL','ELECTRIC','HEAT_PUMP'))
);

CREATE INDEX idx_boiler_owner  ON boiler (owner_type, owner_id);
CREATE INDEX idx_boiler_active ON boiler (owner_type, owner_id, removal_date);

CREATE TABLE boiler_service (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    boiler_id    BIGINT    NOT NULL REFERENCES boiler (id) ON DELETE RESTRICT,
    service_date DATE      NOT NULL,
    valid_until  DATE      NOT NULL,
    notes        TEXT      NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by   BIGINT    REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE INDEX idx_boiler_service_boiler ON boiler_service (boiler_id);
CREATE INDEX idx_boiler_service_date   ON boiler_service (boiler_id, service_date DESC);

-- ─── boiler_service_validity_rule ────────────────────────────────────────────
-- Temporal rules governing how long a boiler service remains valid.
-- Append-only: one rule per valid_from date. The applicable rule for a given
-- service_date is the most recent rule whose valid_from <= service_date.

CREATE TABLE boiler_service_validity_rule (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    valid_from               DATE      NOT NULL UNIQUE,
    validity_duration_months INTEGER   NOT NULL CHECK (validity_duration_months > 0),
    description              TEXT      NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by               BIGINT    REFERENCES app_user (id) ON DELETE SET NULL
);