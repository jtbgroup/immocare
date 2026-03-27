-- ============================================================
-- init-h2.sql — Séquences et objets PostgreSQL-spécifiques
-- recréés en syntaxe H2 pour le profil de développement.
-- Ce fichier est chargé via spring.sql.init.data-locations
-- uniquement avec le profil h2.
-- ============================================================

-- Séquence pour les références de transactions financières
-- (équivalent du SEQUENCE PostgreSQL dans V014__financial_transactions.sql)
CREATE SEQUENCE IF NOT EXISTS financial_transaction_ref_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;