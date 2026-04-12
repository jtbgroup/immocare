-- ============================================================
-- V015 — UC015: Import Parser Strategies
-- Tables: (none — import_parser table created in V014)
-- Seeds: registered parser implementations
-- Note: Parsers are managed via seed data only.
--       No create/edit/delete via API (see BR-UC015-03).
-- ============================================================

INSERT INTO import_parser (code, label, description, format, bank_hint) VALUES
(
    'keytrade-csv-20260102',
    'Keytrade CSV (format jan. 2026)',
    'Colonnes: Date ; Description ; De (counterparty name) ; IBAN (counterparty account) ; Montant — montant positif avec suffixe EUR, direction déduite du préfixe description (vers: = EXPENSE, de: = INCOME), délimiteur ";", encodage UTF-8',
    'CSV',
    'Keytrade'
),
(
    'keytrade-pdf-20260301',
    'Keytrade PDF (format mars 2026)',
    'Extraction multi-lignes par transaction. Montant préfixé + (INCOME) ou - (EXPENSE). Bibliothèque: Apache PDFBox.',
    'PDF',
    'Keytrade'
);
