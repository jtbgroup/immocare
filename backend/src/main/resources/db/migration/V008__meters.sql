-- ============================================================
-- V008 — UC008: Meters (Water, Gas, Electricity)
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

-- Active meter queries (most frequent)
CREATE INDEX idx_meter_owner_active  ON meter (owner_type, owner_id) WHERE end_date IS NULL;
-- History queries
CREATE INDEX idx_meter_owner_history ON meter (owner_type, owner_id, start_date DESC);

COMMENT ON TABLE  meter                     IS 'Append-only utility meter history (WATER, GAS, ELECTRICITY) per housing unit or building';
COMMENT ON COLUMN meter.type                IS 'Enum: WATER, GAS, ELECTRICITY';
COMMENT ON COLUMN meter.meter_number        IS 'Physical meter identifier';
COMMENT ON COLUMN meter.label               IS 'Optional human-readable label (e.g. Kitchen, Basement)';
COMMENT ON COLUMN meter.ean_code            IS 'Required for GAS and ELECTRICITY meters';
COMMENT ON COLUMN meter.installation_number IS 'Required for WATER meters';
COMMENT ON COLUMN meter.customer_number     IS 'Required for WATER meters on a BUILDING';
COMMENT ON COLUMN meter.owner_type          IS 'Enum: HOUSING_UNIT, BUILDING';
COMMENT ON COLUMN meter.owner_id            IS 'FK to housing_unit.id or building.id depending on owner_type';
COMMENT ON COLUMN meter.start_date          IS 'Activation date — cannot be in the future';
COMMENT ON COLUMN meter.end_date            IS 'Closure date — NULL means active';
