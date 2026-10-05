-- Equipment is owned by one management context. A unit's parent building is
-- resolved from the unit when building-level context is needed.
ALTER TABLE fire_extinguisher
    ALTER COLUMN building_id DROP NOT NULL;

UPDATE fire_extinguisher
SET building_id = NULL
WHERE unit_id IS NOT NULL;

ALTER TABLE fire_extinguisher
    DROP CONSTRAINT fire_extinguisher_unit_id_fkey,
    ADD CONSTRAINT fk_fire_extinguisher_unit
        FOREIGN KEY (unit_id) REFERENCES housing_unit (id) ON DELETE RESTRICT,
    DROP CONSTRAINT uq_extinguisher_number,
    ADD CONSTRAINT ck_fire_extinguisher_single_owner
        CHECK ((building_id IS NOT NULL) <> (unit_id IS NOT NULL));

CREATE UNIQUE INDEX uq_fire_extinguisher_building_number
    ON fire_extinguisher (building_id, identification_number)
    WHERE building_id IS NOT NULL;

CREATE UNIQUE INDEX uq_fire_extinguisher_unit_number
    ON fire_extinguisher (unit_id, identification_number)
    WHERE unit_id IS NOT NULL;
