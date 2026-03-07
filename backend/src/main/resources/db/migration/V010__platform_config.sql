-- ============================================================
-- V010 — UC012: Platform Configuration
-- ============================================================

CREATE TABLE platform_config (
    config_key    VARCHAR(100) PRIMARY KEY,
    config_value  VARCHAR(500) NOT NULL,
    value_type    VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    description   TEXT         NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    BIGINT       REFERENCES app_user (id) ON DELETE SET NULL
);

-- ─── Boiler service validity rules (temporal, append-only) ───────────────────

CREATE TABLE boiler_service_validity_rule (
    id                       BIGSERIAL PRIMARY KEY,
    valid_from               DATE      NOT NULL UNIQUE,
    validity_duration_months INTEGER   NOT NULL CHECK (validity_duration_months > 0),
    description              TEXT      NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by               BIGINT    REFERENCES app_user (id) ON DELETE SET NULL
);

-- ─── Default seeds ───────────────────────────────────────────────────────────

INSERT INTO platform_config (config_key, config_value, value_type, description) VALUES
    ('peb_expiry_warning_days',               '90',       'INTEGER', 'Days before PEB certificate expiry to trigger a warning'),
    ('boiler_service_warning_days',           '30',       'INTEGER', 'Days before next boiler service date to trigger an alert'),
    ('lease_end_notice_warning_days',         '30',       'INTEGER', 'Days before lease end notice deadline to trigger an alert'),
    ('indexation_notice_days',                '30',       'INTEGER', 'Days before indexation anniversary to trigger an alert'),
    ('app_name',                              'ImmoCare', 'STRING',  'Application display name'),
    ('default_country',                       'Belgium',  'STRING',  'Default country for new addresses'),
    ('app.date_format',                       'dd/MM/yyyy','STRING', 'Application-wide date display format (Angular date pipe format)'),
    ('boiler.service.alert.threshold.months', '3',        'INTEGER', 'Number of months before service expiry to display a warning alert');

INSERT INTO boiler_service_validity_rule (valid_from, validity_duration_months, description)
VALUES ('1900-01-01', 24, 'Default — 2 years (current regulation)');
