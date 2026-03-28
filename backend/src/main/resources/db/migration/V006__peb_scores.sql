-- ============================================================
-- V006 — UC004: PEB Scores
-- ============================================================

CREATE TABLE peb_score_history (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    housing_unit_id    BIGINT      NOT NULL REFERENCES housing_unit (id) ON DELETE CASCADE,
    peb_score          VARCHAR(10) NOT NULL,
    score_date         DATE        NOT NULL,
    certificate_number VARCHAR(50) NULL,
    valid_until        DATE        NULL,
    created_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_peb_score_value CHECK (peb_score IN ('A_PLUS_PLUS','A_PLUS','A','B','C','D','E','F','G'))
);

CREATE INDEX idx_peb_score_unit_date     ON peb_score_history (housing_unit_id, score_date DESC);
CREATE INDEX idx_peb_score_unit_validity ON peb_score_history (housing_unit_id, valid_until);
