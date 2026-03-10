-- ============================================================
-- V015 — UC009-ext: Person Bank Accounts
-- Links IBAN(s) to a person for financial transaction reconciliation.
-- A person can have multiple IBANs (personal + joint accounts).
-- The counterparty_account on financial_transaction is matched against
-- these IBANs to suggest the related lease automatically.
-- ============================================================

CREATE TABLE person_bank_account (
    id          BIGSERIAL    PRIMARY KEY,
    person_id   BIGINT       NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    iban        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100),
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_person_bank_account_iban UNIQUE (iban)
);

CREATE INDEX idx_pba_person ON person_bank_account (person_id);
CREATE INDEX idx_pba_iban   ON person_bank_account (iban);
