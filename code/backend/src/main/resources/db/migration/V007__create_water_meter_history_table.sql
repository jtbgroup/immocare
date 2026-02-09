-- V007__create_water_meter_history_table.sql
-- Create water meter history table

CREATE TABLE water_meter_history (
    id BIGSERIAL PRIMARY KEY,
    housing_unit_id BIGINT NOT NULL REFERENCES housing_unit(id) ON DELETE CASCADE,
    meter_number VARCHAR(50) NOT NULL,
    meter_location VARCHAR(100),
    installation_date DATE NOT NULL,
    removal_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT removal_after_installation CHECK (removal_date IS NULL OR removal_date >= installation_date)
);

-- Indexes
CREATE INDEX idx_water_meter_housing_unit_install ON water_meter_history(housing_unit_id, installation_date DESC);
CREATE INDEX idx_water_meter_housing_unit_removal ON water_meter_history(housing_unit_id, removal_date);
CREATE INDEX idx_water_meter_active ON water_meter_history(housing_unit_id, removal_date) WHERE removal_date IS NULL;
CREATE INDEX idx_water_meter_number ON water_meter_history(meter_number);

-- Comments
COMMENT ON TABLE water_meter_history IS 'Water meter assignments with installation/removal tracking';
COMMENT ON COLUMN water_meter_history.meter_number IS 'Unique meter identifier (e.g., WM-2024-001)';
COMMENT ON COLUMN water_meter_history.meter_location IS 'Physical location (e.g., Kitchen under sink)';
COMMENT ON COLUMN water_meter_history.installation_date IS 'Date when meter was installed/assigned';
COMMENT ON COLUMN water_meter_history.removal_date IS 'Date when meter was removed (NULL for active meter)';
COMMENT ON CONSTRAINT removal_after_installation ON water_meter_history IS 'Removal date must be after installation';
COMMENT ON INDEX idx_water_meter_active ON water_meter_history IS 'Fast lookup of active meter (removal_date IS NULL)';
