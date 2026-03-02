-- ============================================================
-- V003 — Add application-wide date format configuration key
-- Branch: develop
-- ============================================================

INSERT INTO platform_config (config_key, config_value, description)
VALUES (
    'app.date_format',
    'dd/MM/yyyy',
    'Application-wide date display format. Supported values: dd/MM/yyyy (European), MM/dd/yyyy (American), yyyy-MM-dd (ISO), or any valid Angular date format string.'
);
