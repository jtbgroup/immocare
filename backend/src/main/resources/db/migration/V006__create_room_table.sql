-- V005: Create room table
-- UC003 - Manage Rooms (US012–US016)

CREATE TABLE room (
    id                   BIGSERIAL      PRIMARY KEY,
    housing_unit_id      BIGINT         NOT NULL,
    room_type            VARCHAR(20)    NOT NULL,
    approximate_surface  DECIMAL(6, 2)  NOT NULL,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_room_housing_unit
        FOREIGN KEY (housing_unit_id) REFERENCES housing_unit(id) ON DELETE CASCADE,

    CONSTRAINT chk_room_approximate_surface
        CHECK (approximate_surface > 0 AND approximate_surface < 1000),

    CONSTRAINT chk_room_type
        CHECK (room_type IN (
            'LIVING_ROOM', 'BEDROOM', 'KITCHEN', 'BATHROOM',
            'TOILET', 'HALLWAY', 'STORAGE', 'OFFICE', 'DINING_ROOM', 'OTHER'
        ))
);

CREATE INDEX idx_room_housing_unit_id ON room(housing_unit_id);

COMMENT ON TABLE  room                      IS 'Individual rooms within a housing unit';
COMMENT ON COLUMN room.room_type            IS 'Enum: LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM, TOILET, HALLWAY, STORAGE, OFFICE, DINING_ROOM, OTHER';
COMMENT ON COLUMN room.approximate_surface  IS 'Approximate surface in m² (not legally binding); must be > 0 and < 1000';
