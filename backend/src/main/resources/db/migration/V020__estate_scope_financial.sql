-- ============================================================
-- V020 — UC016: Manage Estates (Phase 4)
-- Adds estate_id to financial tables.
--
-- Strategy for existing data:
--   1. Add column as nullable
--   2. Backfill existing rows with the first available estate
--      (or create a placeholder if none exists yet)
--   3. Enforce NOT NULL
--   4. Add FK constraint and index
--
-- The placeholder estate (if created) is named 'Default Estate'
-- and can be renamed via the admin UI after the migration runs.
-- Note: tag_category is intentionally excluded from the placeholder
--       condition — its rows are seeds with no estate dependency
--       at this stage; backfill happens only when an estate exists.
-- ============================================================

DO $$
DECLARE
    v_estate_id UUID;
BEGIN

    -- Resolve the existing estate used for backfilling, or create a placeholder
    -- only if there are orphan financial rows that actually need it.
    SELECT id INTO v_estate_id FROM estate ORDER BY created_at LIMIT 1;

    IF v_estate_id IS NULL THEN
        IF EXISTS (SELECT 1 FROM bank_account)
           OR EXISTS (SELECT 1 FROM financial_transaction) THEN
            INSERT INTO estate (id, name, created_at)
            VALUES (gen_random_uuid(), 'Default Estate', NOW())
            RETURNING id INTO v_estate_id;
        END IF;
    END IF;

    -- ─── bank_account ────────────────────────────────────────────────────────

    ALTER TABLE bank_account ADD COLUMN IF NOT EXISTS estate_id UUID;
    UPDATE bank_account SET estate_id = v_estate_id WHERE estate_id IS NULL;
    ALTER TABLE bank_account ALTER COLUMN estate_id SET NOT NULL;
    ALTER TABLE bank_account
        ADD CONSTRAINT fk_bank_account_estate
        FOREIGN KEY (estate_id) REFERENCES estate(id) ON DELETE RESTRICT;

    -- ─── tag_category ────────────────────────────────────────────────────────

    ALTER TABLE tag_category ADD COLUMN IF NOT EXISTS estate_id UUID;
    UPDATE tag_category SET estate_id = v_estate_id WHERE estate_id IS NULL;
    ALTER TABLE tag_category ALTER COLUMN estate_id SET NOT NULL;
    ALTER TABLE tag_category
        ADD CONSTRAINT fk_tag_category_estate
        FOREIGN KEY (estate_id) REFERENCES estate(id) ON DELETE RESTRICT;

    -- ─── financial_transaction ────────────────────────────────────────────────

    ALTER TABLE financial_transaction ADD COLUMN IF NOT EXISTS estate_id UUID;
    UPDATE financial_transaction SET estate_id = v_estate_id WHERE estate_id IS NULL;
    ALTER TABLE financial_transaction ALTER COLUMN estate_id SET NOT NULL;
    ALTER TABLE financial_transaction
        ADD CONSTRAINT fk_financial_transaction_estate
        FOREIGN KEY (estate_id) REFERENCES estate(id) ON DELETE RESTRICT;

    -- ─── import_batch (nullable — old batches have no estate) ────────────────

    ALTER TABLE import_batch ADD COLUMN IF NOT EXISTS estate_id UUID;
    ALTER TABLE import_batch
        ADD CONSTRAINT fk_import_batch_estate
        FOREIGN KEY (estate_id) REFERENCES estate(id) ON DELETE SET NULL;

END $$;

-- Indexes created outside PL/pgSQL block
CREATE INDEX IF NOT EXISTS idx_bank_account_estate          ON bank_account(estate_id);
CREATE INDEX IF NOT EXISTS idx_tag_category_estate          ON tag_category(estate_id);
CREATE INDEX IF NOT EXISTS idx_financial_transaction_estate ON financial_transaction(estate_id);
CREATE INDEX IF NOT EXISTS idx_import_batch_estate          ON import_batch(estate_id);