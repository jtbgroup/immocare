-- ============================================================
-- V002 — UC011: Boiler management + UC012: Platform configuration
-- Branch: develop
-- ============================================================

-- ─── Boiler ──────────────────────────────────────────────────────────────────
CREATE TABLE boiler (
    id                  BIGSERIAL       PRIMARY KEY,
    owner_type          VARCHAR(20)     NOT NULL CHECK (owner_type IN ('HOUSING_UNIT', 'BUILDING')),
    owner_id            BIGINT          NOT NULL,
    brand               VARCHAR(100)    NULL,
    model               VARCHAR(100)    NULL,
    serial_number       VARCHAR(100)    NULL,
    fuel_type           VARCHAR(20)     NOT NULL CHECK (fuel_type IN ('GAS', 'OIL', 'ELECTRIC', 'HEAT_PUMP')),
    installation_date   DATE            NOT NULL,
    last_service_date   DATE            NULL,
    next_service_date   DATE            NULL,
    notes               TEXT            NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_boiler_owner ON boiler (owner_type, owner_id);
CREATE INDEX idx_boiler_next_service ON boiler (next_service_date) WHERE next_service_date IS NOT NULL;

-- ─── Platform Config ─────────────────────────────────────────────────────────
CREATE TABLE platform_config (
    config_key      VARCHAR(100)    PRIMARY KEY,
    config_value    TEXT            NOT NULL,
    description     TEXT            NULL,
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Default values
INSERT INTO platform_config (config_key, config_value, description) VALUES
    ('peb_expiry_warning_days',         '90',       'Days before PEB certificate expiry to trigger a warning'),
    ('boiler_service_warning_days',     '30',       'Days before next boiler service date to trigger an alert'),
    ('lease_end_notice_warning_days',   '30',       'Days before lease end notice deadline to trigger an alert'),
    ('indexation_notice_days',          '30',       'Days before indexation anniversary to trigger an alert'),
    ('app_name',                        'ImmoCare', 'Application display name'),
    ('default_country',                 'Belgium',  'Default country for new addresses');
