-- V003__create_housing_units_table.sql
-- Create housing units table

CREATE TABLE housing_unit (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building(id) ON DELETE CASCADE,
    unit_number VARCHAR(20) NOT NULL,
    floor INTEGER NOT NULL,
    landing_number VARCHAR(10),
    total_surface DECIMAL(7,2) CHECK (total_surface > 0),
    has_terrace BOOLEAN NOT NULL DEFAULT FALSE,
    terrace_surface DECIMAL(7,2) CHECK (terrace_surface > 0),
    terrace_orientation VARCHAR(2) CHECK (terrace_orientation IN ('N', 'S', 'E', 'W', 'NE', 'NW', 'SE', 'SW')),
    has_garden BOOLEAN NOT NULL DEFAULT FALSE,
    garden_surface DECIMAL(7,2) CHECK (garden_surface > 0),
    garden_orientation VARCHAR(2) CHECK (garden_orientation IN ('N', 'S', 'E', 'W', 'NE', 'NW', 'SE', 'SW')),
    owner_name VARCHAR(200),
    created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_unit_per_building UNIQUE (building_id, unit_number)
);

-- Indexes
CREATE INDEX idx_housing_unit_building ON housing_unit(building_id);
CREATE INDEX idx_housing_unit_created_by ON housing_unit(created_by);
CREATE INDEX idx_housing_unit_floor ON housing_unit(floor);

-- Comments
COMMENT ON TABLE housing_unit IS 'Individual apartments or units within buildings';
COMMENT ON COLUMN housing_unit.unit_number IS 'Unit identifier (e.g., A101, 1.A, 101)';
COMMENT ON COLUMN housing_unit.floor IS 'Floor number (can be negative for basements)';
COMMENT ON COLUMN housing_unit.total_surface IS 'Total surface in m² (can be calculated or manual)';
COMMENT ON COLUMN housing_unit.owner_name IS 'Unit-specific owner (overrides building owner if set)';
COMMENT ON CONSTRAINT unique_unit_per_building ON housing_unit IS 'Unit number must be unique within building';
