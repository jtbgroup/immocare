-- V006__create_rent_history_table.sql
-- Create rent history table

CREATE TABLE rent_history (
    id BIGSERIAL PRIMARY KEY,
    housing_unit_id BIGINT NOT NULL REFERENCES housing_unit(id) ON DELETE CASCADE,
    monthly_rent DECIMAL(10,2) NOT NULL CHECK (monthly_rent > 0),
    effective_from DATE NOT NULL,
    effective_to DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT effective_to_after_from CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

-- Indexes
CREATE INDEX idx_rent_housing_unit_from ON rent_history(housing_unit_id, effective_from DESC);
CREATE INDEX idx_rent_housing_unit_to ON rent_history(housing_unit_id, effective_to);
CREATE INDEX idx_rent_current ON rent_history(housing_unit_id, effective_to) WHERE effective_to IS NULL;

-- Comments
COMMENT ON TABLE rent_history IS 'Indicative rent amounts with time-based tracking';
COMMENT ON COLUMN rent_history.monthly_rent IS 'Monthly rent in EUR';
COMMENT ON COLUMN rent_history.effective_from IS 'Start date of this rent amount';
COMMENT ON COLUMN rent_history.effective_to IS 'End date (NULL for current rent)';
COMMENT ON COLUMN rent_history.notes IS 'Reason for rent change (e.g., indexation, market adjustment)';
COMMENT ON CONSTRAINT effective_to_after_from ON rent_history IS 'End date must be after start date';
COMMENT ON INDEX idx_rent_current ON rent_history IS 'Fast lookup of current rent (effective_to IS NULL)';
