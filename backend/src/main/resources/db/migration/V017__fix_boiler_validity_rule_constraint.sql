-- ============================================================
-- V017 — Fix boiler_service_validity_rule constraint
-- Removes the global UNIQUE(valid_from) constraint and ensures
-- only the per-estate UNIQUE(estate_id, valid_from) exists.
-- This allows multiple estates to have validity rules on the same date.
-- ============================================================

-- Drop the old global unique constraint if it exists
-- PostgreSQL auto-names it as: boiler_service_validity_rule_valid_from_key
DO $$ BEGIN
  ALTER TABLE boiler_service_validity_rule
    DROP CONSTRAINT IF EXISTS boiler_service_validity_rule_valid_from_key;
EXCEPTION WHEN OTHERS THEN
  -- Constraint might not exist, continue
  NULL;
END $$;

-- Add the correct per-estate constraint if it doesn't exist
DO $$ BEGIN
  ALTER TABLE boiler_service_validity_rule
    ADD CONSTRAINT uq_estate_validity UNIQUE (estate_id, valid_from);
EXCEPTION WHEN OTHERS THEN
  -- Constraint might already exist, continue
  NULL;
END $$;
