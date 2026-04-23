-- ============================================================
-- V009 — UC009: Manage Meters
-- Tables: meter
-- ============================================================

CREATE TABLE meter (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type                VARCHAR(20)  NOT NULL,
    meter_number        VARCHAR(50)  NOT NULL,
    label               VARCHAR(100) NULL,
    ean_code            VARCHAR(18)  NULL,
    installation_number VARCHAR(50)  NULL,
    customer_number     VARCHAR(50)  NULL,
    owner_type          VARCHAR(20)  NOT NULL,
    owner_id            BIGINT       NOT NULL,
    start_date          DATE         NOT NULL,
    end_date            DATE         NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_meter_type            CHECK (type       IN ('WATER','GAS','ELECTRICITY')),
    CONSTRAINT chk_meter_owner_type      CHECK (owner_type IN ('HOUSING_UNIT','BUILDING')),
    CONSTRAINT chk_meter_end_after_start CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_meter_owner_active  ON meter (owner_type, owner_id);
CREATE INDEX idx_meter_owner_history ON meter (owner_type, owner_id, start_date DESC);
