-- ============================================================
-- V009 — UC009: Manage Rents (Housing Unit)
-- Tables: rent_history
-- ============================================================

CREATE TABLE rent_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    housing_unit_id BIGINT        NOT NULL REFERENCES housing_unit (id) ON DELETE CASCADE,
    monthly_rent    NUMERIC(10,2) NOT NULL,
    effective_from  DATE          NOT NULL,
    effective_to    DATE          NULL,
    notes           VARCHAR(500)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_rent_monthly_rent CHECK (monthly_rent > 0),
    CONSTRAINT uq_rent_current       UNIQUE (housing_unit_id, effective_to)
);

CREATE INDEX idx_rent_history_unit_id        ON rent_history (housing_unit_id);
CREATE INDEX idx_rent_history_effective_from ON rent_history (housing_unit_id, effective_from DESC);
