-- ============================================================
-- V013 — UC013: Manage Platform Configuration
-- Tables: platform_config
-- Note: boiler_service_validity_rule belongs to V010 (UC010)
--       as it directly governs boiler service validity logic.
-- ============================================================

CREATE TABLE platform_config (
    config_key    VARCHAR(100) PRIMARY KEY,
    config_value  VARCHAR(500) NOT NULL,
    value_type    VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    description   TEXT         NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    BIGINT       REFERENCES app_user (id) ON DELETE SET NULL
);

-- ─── Seeds: general alert settings ───────────────────────────────────────────
INSERT INTO platform_config (config_key, config_value, value_type, description) VALUES
    ('peb_expiry_warning_days',               '90',        'INTEGER', 'Days before PEB certificate expiry to trigger a warning'),
    ('boiler_service_warning_days',           '30',        'INTEGER', 'Days before next boiler service date to trigger an alert'),
    ('lease_end_notice_warning_days',         '30',        'INTEGER', 'Days before lease end notice deadline to trigger an alert'),
    ('indexation_notice_days',                '30',        'INTEGER', 'Days before indexation anniversary to trigger an alert'),
    ('app_name',                              'ImmoCare',  'STRING',  'Application display name'),
    ('default_country',                       'Belgium',   'STRING',  'Default country for new addresses'),
    ('app.date_format',                       'dd/MM/yyyy','STRING',  'Application-wide date display format (Angular date pipe format)'),
    ('boiler.service.alert.threshold.months', '3',         'INTEGER', 'Number of months before service expiry to display a warning alert'),
    -- Asset type → subcategory mappings (used during transaction import)
    ('asset.type.subcategory.mapping.BOILER',             '', 'STRING', 'Subcategory ID to pre-fill when a BOILER asset link is added (empty = no mapping)'),
    ('asset.type.subcategory.mapping.FIRE_EXTINGUISHER',  '', 'STRING', 'Subcategory ID to pre-fill when a FIRE_EXTINGUISHER asset link is added (empty = no mapping)'),
    ('asset.type.subcategory.mapping.METER',              '', 'STRING', 'Subcategory ID to pre-fill when a METER asset link is added (empty = no mapping)');
