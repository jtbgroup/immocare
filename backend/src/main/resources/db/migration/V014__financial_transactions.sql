-- ============================================================
-- V014 — UC014: Financial Transactions
-- ============================================================

CREATE TABLE bank_account (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    label          VARCHAR(100) NOT NULL,
    account_number VARCHAR(50)  NOT NULL,
    type           VARCHAR(10)  NOT NULL CHECK (type IN ('CURRENT','SAVINGS')),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bank_account_label  UNIQUE (label),
    CONSTRAINT uq_bank_account_number UNIQUE (account_number)
);

CREATE TABLE tag_category (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tag_category_name UNIQUE (name)
);

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

CREATE TABLE import_batch (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    imported_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    filename        VARCHAR(255),
    total_rows      INTEGER      NOT NULL DEFAULT 0,
    imported_count  INTEGER      NOT NULL DEFAULT 0,
    duplicate_count INTEGER      NOT NULL DEFAULT 0,
    error_count     INTEGER      NOT NULL DEFAULT 0,
    created_by      BIGINT       REFERENCES app_user (id) ON DELETE SET NULL
);

CREATE SEQUENCE IF NOT EXISTS financial_transaction_ref_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

CREATE TABLE financial_transaction (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reference            VARCHAR(20)   NOT NULL,
    external_reference   VARCHAR(200),
    transaction_date     DATE          NOT NULL,
    value_date           DATE,
    accounting_month     DATE          NOT NULL,
    amount               DECIMAL(12,2) NOT NULL CHECK (amount > 0),
    direction            VARCHAR(10)   NOT NULL CHECK (direction IN ('INCOME','EXPENSE')),
    description          TEXT,
    counterparty_name    VARCHAR(200),
    counterparty_account VARCHAR(50),
    status               VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                                       CHECK (status IN ('DRAFT','CONFIRMED','RECONCILED')),
    source               VARCHAR(20)   NOT NULL CHECK (source IN ('MANUAL','IMPORT')),
    bank_account_id      BIGINT        REFERENCES bank_account (id) ON DELETE SET NULL,
    subcategory_id       BIGINT        REFERENCES tag_subcategory (id) ON DELETE SET NULL,
    lease_id             BIGINT        REFERENCES lease (id) ON DELETE SET NULL,
    suggested_lease_id   BIGINT        REFERENCES lease (id) ON DELETE SET NULL,
    housing_unit_id      BIGINT        REFERENCES housing_unit (id) ON DELETE SET NULL,
    building_id          BIGINT        REFERENCES building (id) ON DELETE SET NULL,
    import_batch_id      BIGINT        REFERENCES import_batch (id) ON DELETE SET NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
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

CREATE TABLE transaction_asset_link (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id BIGINT      NOT NULL REFERENCES financial_transaction (id) ON DELETE CASCADE,
    asset_type     VARCHAR(30) NOT NULL CHECK (asset_type IN ('BOILER','FIRE_EXTINGUISHER','METER')),
    asset_id       BIGINT      NOT NULL,
    notes          TEXT,
    CONSTRAINT uq_asset_link UNIQUE (transaction_id, asset_type, asset_id)
);

CREATE INDEX idx_tal_transaction ON transaction_asset_link (transaction_id);
CREATE INDEX idx_tal_asset       ON transaction_asset_link (asset_type, asset_id);

CREATE TABLE tag_learning_rule (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    match_field     VARCHAR(30)  NOT NULL
                                 CHECK (match_field IN ('COUNTERPARTY_ACCOUNT','COUNTERPARTY_NAME','DESCRIPTION')),
    match_value     VARCHAR(200) NOT NULL,
    subcategory_id  BIGINT       NOT NULL REFERENCES tag_subcategory (id) ON DELETE CASCADE,
    confidence      INTEGER      NOT NULL DEFAULT 1 CHECK (confidence >= 1),
    last_matched_at TIMESTAMP,
    CONSTRAINT uq_learning_rule UNIQUE (match_field, match_value, subcategory_id)
);

CREATE INDEX idx_learning_rule_field_value ON tag_learning_rule (match_field, match_value);

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

-- ON CONFLICT remplacé par INSERT conditionnel compatible H2 + PostgreSQL
INSERT INTO platform_config (config_key, config_value, value_type, description)
    SELECT 'csv.import.suggestion.confidence.threshold', '3', 'INTEGER',
        'Min confidence score to display a tag suggestion on import'
    WHERE NOT EXISTS (SELECT 1 FROM platform_config WHERE config_key = 'csv.import.suggestion.confidence.threshold');

INSERT INTO platform_config (config_key, config_value, value_type, description)
    SELECT 'import.on_duplicate', 'WARN', 'STRING',
        'Behaviour on duplicate detected: WARN, SKIP or IMPORT'
    WHERE NOT EXISTS (SELECT 1 FROM platform_config WHERE config_key = 'import.on_duplicate');
