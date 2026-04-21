-- ============================================================
-- V021 — UC016: Manage Estates (Phase 5)
-- Modified: boiler_service_validity_rule (add estate_id)
--           platform_config (add estate_id, change PK)
--
-- Strategy for existing data:
--   1. Add columns as nullable
--   2. Create default estate if none exists
--   3. Backfill existing rows with the first available estate
--   4. Enforce NOT NULL
--   5. Add FK constraints and indexes
-- ============================================================

-- 1. Add estate_id to boiler_service_validity_rule
ALTER TABLE boiler_service_validity_rule
    ADD COLUMN estate_id UUID;

-- Backfill: use the first estate (ordered by created_at)
UPDATE boiler_service_validity_rule
    SET estate_id = (SELECT id FROM estate ORDER BY created_at LIMIT 1)
    WHERE estate_id IS NULL;

-- Now enforce NOT NULL and add constraints
ALTER TABLE boiler_service_validity_rule
    ALTER COLUMN estate_id SET NOT NULL,
    ADD CONSTRAINT fk_bsvr_estate FOREIGN KEY (estate_id)
        REFERENCES estate(id) ON DELETE CASCADE;

CREATE INDEX idx_bsvr_estate ON boiler_service_validity_rule(estate_id);

-- Drop old unique constraint on valid_from alone; add estate-scoped uniqueness
ALTER TABLE boiler_service_validity_rule
    DROP CONSTRAINT IF EXISTS boiler_service_validity_rule_valid_from_key;

ALTER TABLE boiler_service_validity_rule
    ADD CONSTRAINT uq_bsvr_estate_valid_from UNIQUE (estate_id, valid_from);

-- 2. Migrate platform_config: add estate_id, change PK to (estate_id, config_key)
ALTER TABLE platform_config
    ADD COLUMN estate_id UUID;

-- Backfill: use the first estate (ordered by created_at)
UPDATE platform_config
    SET estate_id = (SELECT id FROM estate ORDER BY created_at LIMIT 1)
    WHERE estate_id IS NULL;

-- Now enforce NOT NULL and add constraints
ALTER TABLE platform_config
    ALTER COLUMN estate_id SET NOT NULL,
    ADD CONSTRAINT fk_platform_config_estate FOREIGN KEY (estate_id)
        REFERENCES estate(id) ON DELETE CASCADE;

-- Drop old primary key and add new estate-scoped primary key
ALTER TABLE platform_config DROP CONSTRAINT platform_config_pkey;

ALTER TABLE platform_config
    ADD PRIMARY KEY (estate_id, config_key);

CREATE INDEX idx_platform_config_estate ON platform_config(estate_id);
