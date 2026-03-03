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

-- V003 already inserted app.date_format — update its description to clarify scope
UPDATE platform_config
SET description = 'Date display format used throughout the application (alerts, lease details, boiler dates, etc.). Does not affect date pickers in forms — use the calendar widget or type yyyy-MM-dd directly. Supported values: dd/MM/yyyy (European), MM/dd/yyyy (American), yyyy-MM-dd (ISO), or any valid Angular date format string.'
WHERE config_key = 'app.date_format';
