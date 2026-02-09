-- V002__create_buildings_table.sql
-- Create buildings table

CREATE TABLE building (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    street_address VARCHAR(200) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'Belgium',
    owner_name VARCHAR(200),
    created_by BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_building_city ON building(city);
CREATE INDEX idx_building_created_by ON building(created_by);
CREATE INDEX idx_building_name ON building(name);

-- Comments
COMMENT ON TABLE building IS 'Physical buildings containing housing units';
COMMENT ON COLUMN building.owner_name IS 'Building owner (optional, can be inherited by units)';
COMMENT ON COLUMN building.created_by IS 'User who created this building';
