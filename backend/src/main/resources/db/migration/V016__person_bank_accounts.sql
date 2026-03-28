-- ============================================================
-- V016 — UC009-ext: Person Bank Accounts
-- ============================================================

CREATE TABLE person_bank_account (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_id   BIGINT       NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    iban        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100),
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_person_bank_account_iban UNIQUE (iban)
);

CREATE INDEX idx_pba_person ON person_bank_account (person_id);
CREATE INDEX idx_pba_iban   ON person_bank_account (iban);
