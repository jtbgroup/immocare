-- V002: Create building table

CREATE TABLE building (
    id             BIGSERIAL     PRIMARY KEY,
    name           VARCHAR(100)  NOT NULL,
    street_address VARCHAR(200)  NOT NULL,
    postal_code    VARCHAR(20)   NOT NULL,
    city           VARCHAR(100)  NOT NULL,
    country        VARCHAR(100)  NOT NULL DEFAULT 'Belgium',
    owner_name     VARCHAR(200),
    created_by     BIGINT,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_building_created_by FOREIGN KEY (created_by)
        REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE INDEX idx_building_created_by ON building(created_by);
CREATE INDEX idx_building_city       ON building(city);

COMMENT ON TABLE  building            IS 'Physical buildings containing housing units';
COMMENT ON COLUMN building.name       IS 'Building name or identifier';
COMMENT ON COLUMN building.owner_name IS 'Building owner (optional, inherited by housing units)';
COMMENT ON COLUMN building.created_by IS 'User who created this building';
