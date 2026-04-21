-- ============================================================
-- V014 — UC014: Manage Financial Transactions
-- Tables: bank_account, tag_category, tag_subcategory,
--         import_batch, financial_transaction,
--         transaction_asset_link, tag_learning_rule,
--         accounting_month_rule
-- Also includes: import_parser columns on import_batch and
--                financial_transaction (previously split in V015/V017)
-- ============================================================

-- ─── bank_account ─────────────────────────────────────────────────────────────
CREATE TABLE bank_account (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label          VARCHAR(100) NOT NULL,
    account_number VARCHAR(50)  NOT NULL,
    type           VARCHAR(10)  NOT NULL CHECK (type IN ('CURRENT','SAVINGS')),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    owner_user_id  BIGINT       REFERENCES app_user (id) ON DELETE SET NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_bank_account_label  UNIQUE (label),
    CONSTRAINT uq_bank_account_number UNIQUE (account_number)
);

CREATE INDEX idx_bank_account_owner ON bank_account (owner_user_id);

-- ─── tag_category ─────────────────────────────────────────────────────────────
CREATE TABLE tag_category (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_tag_category_name UNIQUE (name)
);

-- ─── tag_subcategory ──────────────────────────────────────────────────────────
CREATE TABLE tag_subcategory (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES tag_category (id) ON DELETE RESTRICT,
    name        VARCHAR(100) NOT NULL,
    direction   VARCHAR(10)  NOT NULL CHECK (direction IN ('INCOME','EXPENSE','BOTH')),
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_tag_subcategory_name_category UNIQUE (category_id, name)
);

CREATE INDEX idx_tag_subcategory_category ON tag_subcategory (category_id);

-- ─── import_parser ────────────────────────────────────────────────────────────
-- Registry of file parser strategies (CSV/PDF).
-- Parsers are managed via seed data — no create/edit/delete via API.
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

-- ─── import_batch ─────────────────────────────────────────────────────────────
CREATE TABLE import_batch (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    imported_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    filename        VARCHAR(255),
    parser_id       BIGINT       REFERENCES import_parser (id) ON DELETE SET NULL,
    bank_account_id BIGINT       REFERENCES bank_account (id)  ON DELETE SET NULL,
    total_rows      INTEGER      NOT NULL DEFAULT 0,
    imported_count  INTEGER      NOT NULL DEFAULT 0,
    duplicate_count INTEGER      NOT NULL DEFAULT 0,
    error_count     INTEGER      NOT NULL DEFAULT 0,
    created_by      BIGINT       REFERENCES app_user (id) ON DELETE SET NULL
);

-- ─── financial_transaction ────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS financial_transaction_ref_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

CREATE TABLE financial_transaction (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reference            VARCHAR(20)    NOT NULL,
    external_reference   VARCHAR(200),
    import_fingerprint   VARCHAR(64),
    transaction_date     DATE           NOT NULL,
    value_date           DATE,
    accounting_month     DATE           NOT NULL,
    amount               DECIMAL(12,2)  NOT NULL CHECK (amount > 0),
    direction            VARCHAR(10)    NOT NULL CHECK (direction IN ('INCOME','EXPENSE')),
    description          TEXT,
    counterparty_account VARCHAR(50),
    status               VARCHAR(20)    NOT NULL DEFAULT 'DRAFT'
                                        CHECK (status IN ('DRAFT','CONFIRMED','RECONCILED','CANCELLED')),
    source               VARCHAR(20)    NOT NULL CHECK (source IN ('MANUAL','IMPORT')),
    bank_account_id      BIGINT         REFERENCES bank_account (id)     ON DELETE SET NULL,
    subcategory_id       BIGINT         REFERENCES tag_subcategory (id)  ON DELETE SET NULL,
    lease_id             BIGINT         REFERENCES lease (id)            ON DELETE SET NULL,
    suggested_lease_id   BIGINT         REFERENCES lease (id)            ON DELETE SET NULL,
    housing_unit_id      BIGINT         REFERENCES housing_unit (id)     ON DELETE SET NULL,
    building_id          BIGINT         REFERENCES building (id)         ON DELETE SET NULL,
    import_batch_id      BIGINT         REFERENCES import_batch (id)     ON DELETE SET NULL,
    parser_id            BIGINT         REFERENCES import_parser (id)    ON DELETE SET NULL,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_financial_transaction_reference UNIQUE (reference)
);

CREATE INDEX idx_ft_transaction_date ON financial_transaction (transaction_date DESC);
CREATE INDEX idx_ft_accounting_month ON financial_transaction (accounting_month DESC);
CREATE INDEX idx_ft_direction        ON financial_transaction (direction);
CREATE INDEX idx_ft_status           ON financial_transaction (status);
CREATE INDEX idx_ft_building         ON financial_transaction (building_id);
CREATE INDEX idx_ft_unit             ON financial_transaction (housing_unit_id);
CREATE INDEX idx_ft_lease            ON financial_transaction (lease_id);
CREATE INDEX idx_ft_import_batch     ON financial_transaction (import_batch_id);
CREATE INDEX idx_ft_external_ref     ON financial_transaction (external_reference);
CREATE INDEX idx_ft_subcategory      ON financial_transaction (subcategory_id);
CREATE INDEX idx_ft_bank_account     ON financial_transaction (bank_account_id);
CREATE INDEX idx_ft_fingerprint      ON financial_transaction (import_fingerprint)
    WHERE import_fingerprint IS NOT NULL;

-- ─── transaction_asset_link ───────────────────────────────────────────────────
-- Links a transaction to a physical asset (BOILER / FIRE_EXTINGUISHER / METER).
-- housing_unit_id and building_id are resolved server-side from the device
-- relationship and are never provided by the client.
-- amount is optional: null = full transaction amount attributed to that asset;
-- when multiple links exist, partial amounts can be entered and their sum
-- must not exceed the transaction total (BR-UC014-15).
CREATE TABLE transaction_asset_link (
    id              BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id  BIGINT        NOT NULL REFERENCES financial_transaction (id) ON DELETE CASCADE,
    asset_type      VARCHAR(30)   NOT NULL CHECK (asset_type IN ('BOILER','FIRE_EXTINGUISHER','METER')),
    asset_id        BIGINT        NOT NULL,
    housing_unit_id BIGINT        REFERENCES housing_unit (id) ON DELETE SET NULL,
    building_id     BIGINT        REFERENCES building (id)     ON DELETE SET NULL,
    amount          DECIMAL(12,2) NULL CHECK (amount > 0),
    notes           TEXT,

    CONSTRAINT uq_asset_link UNIQUE (transaction_id, asset_type, asset_id)
);

CREATE INDEX idx_tal_transaction  ON transaction_asset_link (transaction_id);
CREATE INDEX idx_tal_asset        ON transaction_asset_link (asset_type, asset_id);
CREATE INDEX idx_tal_housing_unit ON transaction_asset_link (housing_unit_id);
CREATE INDEX idx_tal_building     ON transaction_asset_link (building_id);

-- ─── tag_learning_rule ────────────────────────────────────────────────────────
-- Learns subcategory suggestions from past admin confirmations.
-- Valid match_field values: COUNTERPARTY_ACCOUNT, DESCRIPTION, ASSET_TYPE
-- (COUNTERPARTY_NAME is intentionally excluded — see BR-UC014-18)
-- For ASSET_TYPE: match_value is the asset type name (BOILER, FIRE_EXTINGUISHER, METER)
CREATE TABLE tag_learning_rule (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    match_field     VARCHAR(30)  NOT NULL
                                 CHECK (match_field IN ('COUNTERPARTY_ACCOUNT','DESCRIPTION','ASSET_TYPE')),
    match_value     VARCHAR(200) NOT NULL,
    subcategory_id  BIGINT       NOT NULL REFERENCES tag_subcategory (id) ON DELETE CASCADE,
    confidence      INTEGER      NOT NULL DEFAULT 1 CHECK (confidence >= 1),
    last_matched_at TIMESTAMP,

    CONSTRAINT uq_learning_rule UNIQUE (match_field, match_value, subcategory_id)
);

CREATE INDEX idx_learning_rule_field_value ON tag_learning_rule (match_field, match_value);

-- ─── accounting_month_rule ────────────────────────────────────────────────────
CREATE TABLE accounting_month_rule (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subcategory_id       BIGINT      NOT NULL REFERENCES tag_subcategory (id) ON DELETE CASCADE,
    counterparty_account VARCHAR(50),
    month_offset         INTEGER     NOT NULL DEFAULT 0,
    confidence           INTEGER     NOT NULL DEFAULT 1 CHECK (confidence >= 1),
    last_matched_at      TIMESTAMP,

    CONSTRAINT uq_accounting_month_rule UNIQUE (subcategory_id, counterparty_account)
);

CREATE INDEX idx_amr_subcategory ON accounting_month_rule (subcategory_id);