-- ============================================================
-- V003 — UC003: Manage Persons (including Person Bank Accounts)
-- Tables: person, person_bank_account
-- ============================================================

CREATE TABLE person (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    last_name      VARCHAR(100) NOT NULL,
    first_name     VARCHAR(100) NOT NULL,
    birth_date     DATE         NULL,
    birth_place    VARCHAR(100) NULL,
    national_id    VARCHAR(20)  NULL,
    gsm            VARCHAR(20)  NULL,
    email          VARCHAR(100) NULL,
    street_address VARCHAR(200) NULL,
    postal_code    VARCHAR(20)  NULL,
    city           VARCHAR(100) NULL,
    country        VARCHAR(100) NULL DEFAULT 'Belgium',
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_person_national_id UNIQUE (national_id)
);

-- ─── person_bank_account ──────────────────────────────────────────────────────
-- Each person can hold one or more IBANs.
-- Used during transaction import to suggest the related lease
-- when a counterparty account matches a known person IBAN (UC015).

CREATE TABLE person_bank_account (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    person_id  BIGINT       NOT NULL REFERENCES person (id) ON DELETE CASCADE,
    iban       VARCHAR(50)  NOT NULL,
    label      VARCHAR(100) NULL,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_person_bank_account_iban UNIQUE (iban)
);

CREATE INDEX idx_pba_person ON person_bank_account (person_id);
CREATE INDEX idx_pba_iban   ON person_bank_account (iban);
