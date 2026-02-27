-- ============================================================
-- V014: Add inventory registration fields to lease table
--       registration_inventory_spf  : ref number at SPF Finance
--       registration_inventory_region: ref number at the region (Brussels)
-- ============================================================

ALTER TABLE lease
    ADD COLUMN IF NOT EXISTS registration_inventory_spf    VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS registration_inventory_region VARCHAR(100) NULL;
