-- V003: Create housing_unit table

CREATE TABLE housing_unit (
    id                   BIGSERIAL       PRIMARY KEY,
    building_id          BIGINT          NOT NULL,
    unit_number          VARCHAR(20)     NOT NULL,
    floor                INTEGER         NOT NULL,
    landing_number       VARCHAR(10),
    total_surface        DECIMAL(7, 2)   CHECK (total_surface > 0),
    has_terrace          BOOLEAN         NOT NULL DEFAULT FALSE,
    terrace_surface      DECIMAL(7, 2)   CHECK (terrace_surface > 0),
    terrace_orientation  VARCHAR(2),
    has_garden           BOOLEAN         NOT NULL DEFAULT FALSE,
    garden_surface       DECIMAL(7, 2)   CHECK (garden_surface > 0),
    garden_orientation   VARCHAR(2),
    owner_name           VARCHAR(200),
    created_by           BIGINT,
    created_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_housing_unit_building
        FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE,

    CONSTRAINT fk_housing_unit_created_by
        FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL,

    CONSTRAINT uq_housing_unit_number
        UNIQUE (building_id, unit_number),

    CONSTRAINT chk_floor
        CHECK (floor BETWEEN -10 AND 100),

    CONSTRAINT chk_terrace_orientation
        CHECK (terrace_orientation IN ('N','S','E','W','NE','NW','SE','SW')),

    CONSTRAINT chk_garden_orientation
        CHECK (garden_orientation IN ('N','S','E','W','NE','NW','SE','SW'))
);

CREATE INDEX idx_housing_unit_building_id ON housing_unit(building_id);
CREATE INDEX idx_housing_unit_created_by  ON housing_unit(created_by);

COMMENT ON TABLE  housing_unit                   IS 'Individual apartments / units within a building';
COMMENT ON COLUMN housing_unit.unit_number        IS 'Unit identifier, unique within a building (e.g. A101, 1B)';
COMMENT ON COLUMN housing_unit.floor              IS 'Floor number; negative values indicate underground levels';
COMMENT ON COLUMN housing_unit.total_surface      IS 'Total surface in m²; may be entered manually or derived from rooms';
COMMENT ON COLUMN housing_unit.terrace_orientation IS 'Cardinal orientation of the terrace (N,S,E,W,NE,NW,SE,SW)';
COMMENT ON COLUMN housing_unit.garden_orientation  IS 'Cardinal orientation of the garden (N,S,E,W,NE,NW,SE,SW)';
COMMENT ON COLUMN housing_unit.owner_name         IS 'Unit-specific owner; overrides building.owner_name when set';
