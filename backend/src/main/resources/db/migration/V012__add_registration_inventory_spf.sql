-- ============================================================
-- V012: Add registration_inventory_spf column to lease table
-- ============================================================

ALTER TABLE lease
    ADD COLUMN IF NOT EXISTS registration_inventory_spf VARCHAR(50) NULL;
