-- ============================================================
-- V013: Replace lease_indexation_history with lease_rent_adjustment
--       Also drop indexation-specific columns from lease table.
-- ============================================================

DROP TABLE IF EXISTS lease_indexation_history;

ALTER TABLE lease
    DROP COLUMN IF EXISTS indexation_notice_days,
    DROP COLUMN IF EXISTS indexation_anniversary_month,
    DROP COLUMN IF EXISTS base_index_value,
    DROP COLUMN IF EXISTS base_index_month;

CREATE TABLE lease_rent_adjustment (
    id             BIGSERIAL     PRIMARY KEY,
    lease_id       BIGINT        NOT NULL REFERENCES lease(id) ON DELETE CASCADE,
    field          VARCHAR(10)   NOT NULL,          -- 'RENT' or 'CHARGES'
    old_value      NUMERIC(10,2) NOT NULL,
    new_value      NUMERIC(10,2) NOT NULL,
    reason         TEXT          NOT NULL,
    effective_date DATE          NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rent_adj_field CHECK (field IN ('RENT','CHARGES'))
);

CREATE INDEX idx_rent_adj_lease ON lease_rent_adjustment (lease_id, effective_date DESC);
