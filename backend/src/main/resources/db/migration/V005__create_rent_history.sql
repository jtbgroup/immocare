-- UC005 - Manage Rents
-- Append-only rent history table for housing units
-- Business Rule: current rent has effective_to = NULL (only one per unit allowed)

CREATE TABLE rent_history (
    id               BIGSERIAL PRIMARY KEY,
    housing_unit_id  BIGINT        NOT NULL REFERENCES housing_unit(id) ON DELETE CASCADE,
    monthly_rent     NUMERIC(10,2) NOT NULL CHECK (monthly_rent > 0),
    effective_from   DATE          NOT NULL,
    effective_to     DATE          NULL,
    notes            VARCHAR(500)  NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- Only one active (current) rent per unit
    CONSTRAINT uq_rent_current UNIQUE (housing_unit_id, effective_to) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_rent_history_unit_id ON rent_history(housing_unit_id);
CREATE INDEX idx_rent_history_effective_from ON rent_history(housing_unit_id, effective_from DESC);
