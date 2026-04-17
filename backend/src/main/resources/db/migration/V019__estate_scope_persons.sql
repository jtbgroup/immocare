-- ============================================================
-- V019 — UC016: Manage Estates (Phase 3)
-- Modified: person (add estate_id)
-- Note: lease derives its estate scope via housing_unit → building.estate_id
--       no column added to lease, lease_tenant, or person_bank_account
-- ============================================================

ALTER TABLE person
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_person_estate ON person(estate_id);
