-- ============================================================
-- V010: Drop owner_name columns (run AFTER validating V009)
-- ============================================================

ALTER TABLE building      DROP COLUMN IF EXISTS owner_name;
ALTER TABLE housing_unit  DROP COLUMN IF EXISTS owner_name;
