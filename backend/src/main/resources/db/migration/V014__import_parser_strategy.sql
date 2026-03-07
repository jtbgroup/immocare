-- ============================================================
-- V014 — Import parser strategies + fingerprint deduplication
-- ============================================================

-- ─── 1. import_parser ────────────────────────────────────────────────────────
-- Registre des stratégies d'import. Chaque parseur est identifié par un code
-- unique et correspond à une classe Java dédiée.

CREATE TABLE import_parser (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(60)  NOT NULL,   -- ex: keytrade-csv-20260102
    label       VARCHAR(100) NOT NULL,   -- ex: Keytrade CSV (jan. 2026)
    description TEXT,                   -- colonnes attendues, notes
    format      VARCHAR(10)  NOT NULL CHECK (format IN ('CSV', 'PDF')),
    bank_hint   VARCHAR(100),           -- nom de banque indicatif
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_import_parser_code UNIQUE (code)
);

-- Seed: deux premiers parseurs
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

-- ─── 2. import_batch: lier au parseur choisi ─────────────────────────────────
ALTER TABLE import_batch
    ADD COLUMN parser_id     BIGINT REFERENCES import_parser(id) ON DELETE SET NULL,
    ADD COLUMN bank_account_id BIGINT REFERENCES bank_account(id) ON DELETE SET NULL;

-- ─── 3. financial_transaction: fingerprint + parser source ───────────────────
-- import_fingerprint: SHA-256(date||amount||counterparty_account||description[:50])
-- Permet la détection de doublons sans référence externe.
ALTER TABLE financial_transaction
    ADD COLUMN import_fingerprint VARCHAR(64),
    ADD COLUMN parser_id          BIGINT REFERENCES import_parser(id) ON DELETE SET NULL;

-- Mettre à jour le CHECK sur source pour inclure IMPORT (générique)
ALTER TABLE financial_transaction
    DROP CONSTRAINT IF EXISTS financial_transaction_source_check;
ALTER TABLE financial_transaction
    ADD CONSTRAINT financial_transaction_source_check
    CHECK (source IN ('MANUAL', 'IMPORT'));

-- Index sur le fingerprint pour détection rapide de doublons
CREATE INDEX idx_ft_fingerprint ON financial_transaction (import_fingerprint)
    WHERE import_fingerprint IS NOT NULL;

-- ─── 4. bank_account: lier à un utilisateur (propriétaire) ──────────────────
-- owner_user_id NULL = compte système/partagé
-- owner_user_id SET  = compte personnel de l'utilisateur
ALTER TABLE bank_account
    ADD COLUMN owner_user_id BIGINT REFERENCES app_user(id) ON DELETE SET NULL;

CREATE INDEX idx_bank_account_owner ON bank_account (owner_user_id);

-- ─── 5. Supprimer les clés platform_config csv.import.col.* ─────────────────
DELETE FROM platform_config WHERE config_key IN (
    'csv.import.delimiter',
    'csv.import.date_format',
    'csv.import.skip_header_rows',
    'csv.import.col.date',
    'csv.import.col.amount',
    'csv.import.col.description',
    'csv.import.col.counterparty_name',
    'csv.import.col.counterparty_account',
    'csv.import.col.external_reference',
    'csv.import.col.bank_account',
    'csv.import.col.value_date'
);

-- Garder et renommer les 2 clés résiduelles utiles
UPDATE platform_config
    SET description = 'Seuil de confiance minimum pour afficher une suggestion de catégorie (entier ≥ 1)'
    WHERE config_key = 'csv.import.suggestion.confidence.threshold';

-- Ajouter la clé comportement sur doublon
INSERT INTO platform_config (config_key, config_value, description) VALUES
    ('import.on_duplicate', 'WARN', 'Comportement si doublon détecté: WARN, SKIP ou IMPORT')
ON CONFLICT (config_key) DO NOTHING;

