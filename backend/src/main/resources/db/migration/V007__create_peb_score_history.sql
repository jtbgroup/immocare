-- V007: Create peb_score_history table
-- UC004 - Manage PEB Scores (US017-US020)
-- Append-only: current score = record with most recent score_date

CREATE TABLE peb_score_history (
    id                 BIGSERIAL     PRIMARY KEY,
    housing_unit_id    BIGINT        NOT NULL,
    peb_score          VARCHAR(10)   NOT NULL,
    score_date         DATE          NOT NULL,
    certificate_number VARCHAR(50)   NULL,
    valid_until        DATE          NULL,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_peb_score_housing_unit
        FOREIGN KEY (housing_unit_id) REFERENCES housing_unit(id) ON DELETE CASCADE,

    CONSTRAINT chk_peb_score_value
        CHECK (peb_score IN ('A_PLUS_PLUS', 'A_PLUS', 'A', 'B', 'C', 'D', 'E', 'F', 'G'))
);

CREATE INDEX idx_peb_score_unit_date     ON peb_score_history(housing_unit_id, score_date DESC);
CREATE INDEX idx_peb_score_unit_validity ON peb_score_history(housing_unit_id, valid_until);

COMMENT ON TABLE  peb_score_history                    IS 'Append-only PEB energy certificate history per housing unit';
COMMENT ON COLUMN peb_score_history.peb_score          IS 'Enum: A_PLUS_PLUS, A_PLUS, A, B, C, D, E, F, G';
COMMENT ON COLUMN peb_score_history.score_date         IS 'Date of certificate issuance; cannot be in the future';
COMMENT ON COLUMN peb_score_history.certificate_number IS 'Optional certificate reference number (alphanumeric + hyphens)';
COMMENT ON COLUMN peb_score_history.valid_until        IS 'Optional expiry date; must be after score_date if set';
