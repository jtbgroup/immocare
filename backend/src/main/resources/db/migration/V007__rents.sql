-- ============================================================
-- V007 — UC005: Rents (Housing Unit)
-- ============================================================

CREATE TABLE rent_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    housing_unit_id BIGINT        NOT NULL REFERENCES housing_unit (id) ON DELETE CASCADE,
    monthly_rent    NUMERIC(10,2) NOT NULL CHECK (monthly_rent > 0),
    effective_from  DATE          NOT NULL,
    effective_to    DATE          NULL,
    notes           VARCHAR(500)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- Only one active (current) rent per unit
    -- DEFERRABLE INITIALLY DEFERRED supprimé : non supporté par H2
    CONSTRAINT uq_rent_current UNIQUE (housing_unit_id, effective_to)
);

CREATE INDEX idx_rent_history_unit_id        ON rent_history (housing_unit_id);
CREATE INDEX idx_rent_history_effective_from ON rent_history (housing_unit_id, effective_from DESC);
