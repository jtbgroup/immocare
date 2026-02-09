-- V005__create_peb_score_history_table.sql
-- Create PEB score history table

CREATE TABLE peb_score_history (
    id BIGSERIAL PRIMARY KEY,
    housing_unit_id BIGINT NOT NULL REFERENCES housing_unit(id) ON DELETE CASCADE,
    peb_score VARCHAR(10) NOT NULL CHECK (peb_score IN (
        'A_PLUS_PLUS', 'A_PLUS', 'A', 'B', 'C', 'D', 'E', 'F', 'G'
    )),
    score_date DATE NOT NULL,
    certificate_number VARCHAR(50),
    valid_until DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_until_after_score_date CHECK (valid_until IS NULL OR valid_until > score_date)
);

-- Indexes
CREATE INDEX idx_peb_housing_unit_date ON peb_score_history(housing_unit_id, score_date DESC);
CREATE INDEX idx_peb_valid_until ON peb_score_history(housing_unit_id, valid_until);
CREATE INDEX idx_peb_certificate ON peb_score_history(certificate_number);

-- Comments
COMMENT ON TABLE peb_score_history IS 'Energy Performance Certificate (PEB) scores with history';
COMMENT ON COLUMN peb_score_history.peb_score IS 'Energy score: A++ (best) to G (worst)';
COMMENT ON COLUMN peb_score_history.score_date IS 'Date when certificate was issued';
COMMENT ON COLUMN peb_score_history.valid_until IS 'Certificate expiration date (typically 10 years)';
COMMENT ON CONSTRAINT valid_until_after_score_date ON peb_score_history IS 'Certificate cannot expire before issuance';
