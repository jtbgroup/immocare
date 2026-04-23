-- ============================================================
-- V013 — UC013: Manage Platform Configuration
-- Tables: platform_config
-- Note: boiler_service_validity_rule belongs to V010 (UC011)
--       as it directly governs boiler service validity logic.
-- ============================================================

CREATE TABLE platform_config (
    estate_id     UUID NOT NULL REFERENCES estate(id),
    config_key    VARCHAR(100) NOT NULL,
    config_value  VARCHAR(500) NOT NULL,
    value_type    VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    description   TEXT         NULL,
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    BIGINT       REFERENCES app_user (id) ON DELETE SET NULL,
    CONSTRAINT pk_platform_config PRIMARY KEY (estate_id, config_key)
);