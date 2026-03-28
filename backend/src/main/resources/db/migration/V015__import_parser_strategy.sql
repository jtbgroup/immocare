-- ============================================================
-- V015 — UC015: Import Parser Strategies
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
    'Keytrade CSV (format fév. 2026)',
    'Colonnes: Date;Description;De;IBAN;Montant — montant positif, suffixe EUR, direction manuelle, séparateur ";"',
    'CSV',
    'Keytrade'
),
(
    'keytrade-pdf-20260301',
    'Keytrade PDF (format mars 2026)',
    'Relevé PDF Keytrade — blocs multi-lignes, signe +/- explicite, "vers:" = EXPENSE, "de:" = INCOME',
    'PDF',
    'Keytrade'
);

ALTER TABLE import_batch
    ADD COLUMN parser_id       BIGINT REFERENCES import_parser (id) ON DELETE SET NULL;
ALTER TABLE import_batch
    ADD COLUMN bank_account_id BIGINT REFERENCES bank_account (id)  ON DELETE SET NULL;

ALTER TABLE financial_transaction
    ADD COLUMN import_fingerprint VARCHAR(64);
ALTER TABLE financial_transaction
    ADD COLUMN parser_id          BIGINT REFERENCES import_parser (id) ON DELETE SET NULL;

-- Index partiel WHERE supprimé (non supporté par H2)
CREATE INDEX idx_ft_fingerprint ON financial_transaction (import_fingerprint);

ALTER TABLE bank_account
    ADD COLUMN owner_user_id BIGINT REFERENCES app_user (id) ON DELETE SET NULL;

CREATE INDEX idx_bank_account_owner ON bank_account (owner_user_id);
