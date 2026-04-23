-- ============================================================
-- V006 — UC007: Manage Rooms
-- Tables: room
-- ============================================================

CREATE TABLE room (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    housing_unit_id     BIGINT       NOT NULL REFERENCES housing_unit (id) ON DELETE CASCADE,
    room_type           VARCHAR(20)  NOT NULL,
    approximate_surface DECIMAL(6,2) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_room_approximate_surface CHECK (approximate_surface > 0 AND approximate_surface < 1000),
    CONSTRAINT chk_room_type CHECK (room_type IN (
        'LIVING_ROOM','BEDROOM','KITCHEN','BATHROOM',
        'TOILET','HALLWAY','STORAGE','OFFICE','DINING_ROOM','OTHER'
    ))
);

CREATE INDEX idx_room_housing_unit_id ON room (housing_unit_id);
