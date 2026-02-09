-- V004__create_rooms_table.sql
-- Create rooms table

CREATE TABLE room (
    id BIGSERIAL PRIMARY KEY,
    housing_unit_id BIGINT NOT NULL REFERENCES housing_unit(id) ON DELETE CASCADE,
    room_type VARCHAR(20) NOT NULL CHECK (room_type IN (
        'LIVING_ROOM', 'BEDROOM', 'KITCHEN', 'BATHROOM', 'TOILET',
        'HALLWAY', 'STORAGE', 'OFFICE', 'DINING_ROOM', 'OTHER'
    )),
    approximate_surface DECIMAL(6,2) NOT NULL CHECK (approximate_surface > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_room_housing_unit ON room(housing_unit_id);
CREATE INDEX idx_room_type ON room(room_type);

-- Comments
COMMENT ON TABLE room IS 'Individual rooms within housing units';
COMMENT ON COLUMN room.room_type IS 'Type of room (enumeration)';
COMMENT ON COLUMN room.approximate_surface IS 'Approximate surface in m² (not legally binding)';
