-- ============================================================
-- V005 — UC005: Manage Housing Units
-- Tables: housing_unit
-- ============================================================

CREATE TABLE housing_unit (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    building_id         BIGINT        NOT NULL REFERENCES building (id) ON DELETE CASCADE,
    unit_number         VARCHAR(20)   NOT NULL,
    floor               INTEGER       NOT NULL,
    landing_number      VARCHAR(10)   NULL,
    total_surface       DECIMAL(7,2)  NULL,
    has_terrace         BOOLEAN       NOT NULL DEFAULT FALSE,
    terrace_surface     DECIMAL(7,2)  NULL,
    terrace_orientation VARCHAR(2)    NULL,
    has_garden          BOOLEAN       NOT NULL DEFAULT FALSE,
    garden_surface      DECIMAL(7,2)  NULL,
    garden_orientation  VARCHAR(2)    NULL,
    owner_id            BIGINT        NULL REFERENCES person (id) ON DELETE SET NULL,
    created_by          BIGINT        NULL REFERENCES app_user (id) ON DELETE SET NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_housing_unit_number  UNIQUE (building_id, unit_number),
    CONSTRAINT chk_floor               CHECK (floor BETWEEN -10 AND 100),
    CONSTRAINT chk_total_surface       CHECK (total_surface > 0),
    CONSTRAINT chk_terrace_surface     CHECK (terrace_surface > 0),
    CONSTRAINT chk_garden_surface      CHECK (garden_surface > 0),
    CONSTRAINT chk_terrace_orientation CHECK (terrace_orientation IN ('N','S','E','W','NE','NW','SE','SW')),
    CONSTRAINT chk_garden_orientation  CHECK (garden_orientation  IN ('N','S','E','W','NE','NW','SE','SW'))
);

CREATE INDEX idx_housing_unit_building_id ON housing_unit (building_id);
CREATE INDEX idx_housing_unit_owner       ON housing_unit (owner_id);
CREATE INDEX idx_housing_unit_created_by  ON housing_unit (created_by);
