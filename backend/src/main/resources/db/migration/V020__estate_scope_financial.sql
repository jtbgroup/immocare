-- ============================================================
-- V020 — UC016: Manage Estates (Phase 4)
-- Modified: bank_account, tag_category, financial_transaction
-- Note: tag_subcategory scoped via tag_category.estate_id
--       import_batch scoped via financial_transaction (no FK added)
--       tag_learning_rule and accounting_month_rule scoped via tag_subcategory
-- ============================================================

-- 1. Add estate_id to bank_account
ALTER TABLE bank_account
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_bank_account_estate ON bank_account(estate_id);

-- 2. Add estate_id to tag_category
ALTER TABLE tag_category
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_tag_category_estate ON tag_category(estate_id);

-- 3. Add estate_id to financial_transaction
ALTER TABLE financial_transaction
    ADD COLUMN estate_id UUID NOT NULL
        REFERENCES estate(id) ON DELETE RESTRICT;

CREATE INDEX idx_financial_transaction_estate ON financial_transaction(estate_id);

-- 4. Add estate_id to import_batch (for scoped import history)
ALTER TABLE import_batch
    ADD COLUMN estate_id UUID
        REFERENCES estate(id) ON DELETE SET NULL;

CREATE INDEX idx_import_batch_estate ON import_batch(estate_id);
