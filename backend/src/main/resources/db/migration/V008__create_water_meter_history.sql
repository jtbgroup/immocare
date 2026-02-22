-- V008: Create water_meter_history table
-- UC006 - Manage Water Meters (US026-US030)
-- Append-only: active meter = record with removal_date = NULL
-- Only one active meter per housing unit at any time

CREATE TABLE water_meter_history (
    id                BIGSERIAL     PRIMARY KEY,
    housing_unit_id   BIGINT        NOT NULL,
    meter_number      VARCHAR(50)   NOT NULL,
    meter_location    VARCHAR(100)  NULL,
    installation_date DATE          NOT NULL,
    removal_date      DATE          NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_water_meter_housing_unit
        FOREIGN KEY (housing_unit_id) REFERENCES housing_unit(id) ON DELETE CASCADE,

    CONSTRAINT chk_removal_after_installation
        CHECK (removal_date IS NULL OR removal_date >= installation_date)
);

-- Fast lookup of active meter per unit (removal_date IS NULL)
CREATE INDEX idx_water_meter_unit_active      ON water_meter_history(housing_unit_id, removal_date);
-- Fast history retrieval sorted by date
CREATE INDEX idx_water_meter_unit_date        ON water_meter_history(housing_unit_id, installation_date DESC);
-- Meter number lookup
CREATE INDEX idx_water_meter_number           ON water_meter_history(meter_number);

COMMENT ON TABLE  water_meter_history                      IS 'Append-only water meter history per housing unit';
COMMENT ON COLUMN water_meter_history.meter_number         IS 'Meter identifier, alphanumeric with hyphens/underscores, max 50 chars';
COMMENT ON COLUMN water_meter_history.meter_location       IS 'Optional physical location (e.g. Kitchen under sink), max 100 chars';
COMMENT ON COLUMN water_meter_history.installation_date    IS 'Date meter was installed; cannot be in the future';
COMMENT ON COLUMN water_meter_history.removal_date         IS 'Date meter was removed/replaced; NULL = currently active';
