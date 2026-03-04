-- ─── fire_extinguisher ────────────────────────────────────────────────────────

CREATE TABLE fire_extinguisher (
    id                    BIGSERIAL    PRIMARY KEY,
    building_id           BIGINT       NOT NULL REFERENCES building (id) ON DELETE CASCADE,
    unit_id               BIGINT       NULL     REFERENCES housing_unit (id) ON DELETE SET NULL,
    identification_number VARCHAR(50)  NOT NULL,
    notes                 TEXT         NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_extinguisher_number UNIQUE (building_id, identification_number)
);

CREATE INDEX idx_fire_extinguisher_building ON fire_extinguisher (building_id);
CREATE INDEX idx_fire_extinguisher_unit     ON fire_extinguisher (unit_id);

COMMENT ON TABLE  fire_extinguisher                       IS 'Fire extinguishers attached to a building';
COMMENT ON COLUMN fire_extinguisher.identification_number IS 'Unique identifier within the building';
COMMENT ON COLUMN fire_extinguisher.unit_id               IS 'Optional FK — housing unit where the extinguisher is located';

-- ─── fire_extinguisher_revision ───────────────────────────────────────────────

CREATE TABLE fire_extinguisher_revision (
    id                    BIGSERIAL  PRIMARY KEY,
    fire_extinguisher_id  BIGINT     NOT NULL REFERENCES fire_extinguisher (id) ON DELETE CASCADE,
    revision_date         DATE       NOT NULL,
    notes                 TEXT       NULL,
    created_at            TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fire_ext_revision_extinguisher ON fire_extinguisher_revision (fire_extinguisher_id);

COMMENT ON TABLE fire_extinguisher_revision IS 'Revision history for a fire extinguisher';
