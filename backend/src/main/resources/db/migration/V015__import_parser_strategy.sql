-- ============================================================
-- V015 — UC015: Import Parser Strategies
-- Use case: UC-015
-- ============================================================

CREATE TABLE import_parser (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(60)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    format      VARCHAR(10)  NOT NULL CHECK (format IN ('CSV', 'PDF')),
    bank_hint   VARCHAR(100),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_import_parser_code UNIQUE (code)
);

INSERT INTO import_parser (code, label, description, format, bank_hint) VALUES
(
    'keytrade-csv-20260102',
    'Keytrade CSV (format jan. 2026 — 7 colonnes)',
    'Colonnes: Extrait;Date;Date valeur;IBAN;Description;Montant;Devise — montant signé (positif=INCOME, négatif=EXPENSE), séparateur ";", encodage UTF-8 BOM',
    'CSV',
    'Keytrade'
);

ALTER TABLE import_batch
    ADD COLUMN parser_id       BIGINT REFERENCES import_parser (id) ON DELETE SET NULL;
ALTER TABLE import_batch
    ADD COLUMN bank_account_id BIGINT REFERENCES bank_account (id)  ON DELETE SET NULL;


ALTER TABLE financial_transaction
    ADD COLUMN parser_id          BIGINT REFERENCES import_parser (id) ON DELETE SET NULL;


ALTER TABLE bank_account
    ADD COLUMN owner_user_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL;

CREATE INDEX idx_bank_account_owner ON bank_account (owner_user_id);