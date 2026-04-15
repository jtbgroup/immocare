-- ============================================================
-- V018 — UC016: Manage Estates (Phase 2)
-- Modified: building (add estate_id)
-- ============================================================

-- Add estate_id to building
ALTER TABLE building
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_building_estate ON building(estate_id);
