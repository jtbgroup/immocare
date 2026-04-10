-- ============================================================
-- V017 — UC014 patch: transaction_asset_link missing columns
-- Adds housing_unit_id, building_id, amount columns that were
-- missing from the initial V014 migration already applied.
-- Also corrects the tag_learning_rule match_field CHECK constraint
-- to include ASSET_TYPE and remove COUNTERPARTY_NAME.
-- ============================================================

ALTER TABLE transaction_asset_link
    ADD COLUMN IF NOT EXISTS housing_unit_id BIGINT REFERENCES housing_unit (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS building_id     BIGINT REFERENCES building (id)      ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS amount          DECIMAL(12,2) NULL CHECK (amount > 0);

CREATE INDEX IF NOT EXISTS idx_tal_housing_unit ON transaction_asset_link (housing_unit_id);
CREATE INDEX IF NOT EXISTS idx_tal_building     ON transaction_asset_link (building_id);

-- Fix the match_field CHECK constraint on tag_learning_rule:
-- remove COUNTERPARTY_NAME, add ASSET_TYPE (BR-UC014-18)
ALTER TABLE tag_learning_rule DROP CONSTRAINT IF EXISTS tag_learning_rule_match_field_check;
ALTER TABLE tag_learning_rule
    ADD CONSTRAINT tag_learning_rule_match_field_check
    CHECK (match_field IN ('COUNTERPARTY_ACCOUNT', 'DESCRIPTION', 'ASSET_TYPE'));
