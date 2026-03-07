-- ============================================================
-- V013 — UC014: Financial Transactions
-- ============================================================

-- ─── bank_account ────────────────────────────────────────────────────────────

CREATE TABLE bank_account (
    id             BIGSERIAL    PRIMARY KEY,
    label          VARCHAR(100) NOT NULL,
    account_number VARCHAR(50)  NOT NULL,
    type           VARCHAR(10)  NOT NULL CHECK (type IN ('CURRENT','SAVINGS')),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_bank_account_label  UNIQUE (label),
    CONSTRAINT uq_bank_account_number UNIQUE (account_number)
);

-- ─── tag_category ────────────────────────────────────────────────────────────

CREATE TABLE tag_category (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tag_category_name UNIQUE (name)
);

-- ─── tag_subcategory ─────────────────────────────────────────────────────────

CREATE TABLE tag_subcategory (
    id          BIGSERIAL   PRIMARY KEY,
    category_id BIGINT      NOT NULL REFERENCES tag_category (id) ON DELETE RESTRICT,
    name        VARCHAR(100) NOT NULL,
    direction   VARCHAR(10)  NOT NULL CHECK (direction IN ('INCOME','EXPENSE','BOTH')),
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tag_subcategory_name_category UNIQUE (category_id, name)
);

CREATE INDEX idx_tag_subcategory_category ON tag_subcategory (category_id);

-- ─── import_batch ────────────────────────────────────────────────────────────

CREATE TABLE import_batch (
    id              BIGSERIAL   PRIMARY KEY,
    imported_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    filename        VARCHAR(255),
    total_rows      INTEGER     NOT NULL DEFAULT 0,
    imported_count  INTEGER     NOT NULL DEFAULT 0,
    duplicate_count INTEGER     NOT NULL DEFAULT 0,
    error_count     INTEGER     NOT NULL DEFAULT 0,
    created_by      BIGINT      REFERENCES app_user (id) ON DELETE SET NULL
);

-- ─── financial_transaction ───────────────────────────────────────────────────

CREATE TABLE financial_transaction (
    id                   BIGSERIAL     PRIMARY KEY,
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
    source               VARCHAR(20)   NOT NULL CHECK (source IN ('MANUAL','CSV_IMPORT')),
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

-- ─── transaction_asset_link ──────────────────────────────────────────────────

CREATE TABLE transaction_asset_link (
    id             BIGSERIAL   PRIMARY KEY,
    transaction_id BIGINT      NOT NULL REFERENCES financial_transaction (id) ON DELETE CASCADE,
    asset_type     VARCHAR(30) NOT NULL CHECK (asset_type IN ('BOILER','FIRE_EXTINGUISHER','METER')),
    asset_id       BIGINT      NOT NULL,
    notes          TEXT,
    CONSTRAINT uq_asset_link UNIQUE (transaction_id, asset_type, asset_id)
);

CREATE INDEX idx_tal_transaction ON transaction_asset_link (transaction_id);
CREATE INDEX idx_tal_asset       ON transaction_asset_link (asset_type, asset_id);

-- ─── tag_learning_rule ───────────────────────────────────────────────────────

CREATE TABLE tag_learning_rule (
    id              BIGSERIAL    PRIMARY KEY,
    match_field     VARCHAR(30)  NOT NULL
                                 CHECK (match_field IN ('COUNTERPARTY_ACCOUNT','COUNTERPARTY_NAME','DESCRIPTION')),
    match_value     VARCHAR(200) NOT NULL,
    subcategory_id  BIGINT       NOT NULL REFERENCES tag_subcategory (id) ON DELETE CASCADE,
    confidence      INTEGER      NOT NULL DEFAULT 1 CHECK (confidence >= 1),
    last_matched_at TIMESTAMP,
    CONSTRAINT uq_learning_rule UNIQUE (match_field, match_value, subcategory_id)
);

CREATE INDEX idx_learning_rule_field_value ON tag_learning_rule (match_field, match_value);

-- ─── accounting_month_rule ───────────────────────────────────────────────────

CREATE TABLE accounting_month_rule (
    id                   BIGSERIAL   PRIMARY KEY,
    subcategory_id       BIGINT      NOT NULL REFERENCES tag_subcategory (id) ON DELETE CASCADE,
    counterparty_account VARCHAR(50),
    month_offset         INTEGER     NOT NULL DEFAULT 0,
    confidence           INTEGER     NOT NULL DEFAULT 1 CHECK (confidence >= 1),
    last_matched_at      TIMESTAMP,
    CONSTRAINT uq_accounting_month_rule UNIQUE (subcategory_id, counterparty_account)
);

CREATE INDEX idx_amr_subcategory ON accounting_month_rule (subcategory_id);

-- ─── platform_config additions for CSV import ────────────────────────────────

INSERT INTO platform_config (config_key, config_value, value_type, description) VALUES
    ('csv.import.delimiter',                       ';',          'STRING',  'CSV column delimiter'),
    ('csv.import.date_format',                     'dd/MM/yyyy', 'STRING',  'Date format in CSV'),
    ('csv.import.skip_header_rows',                '1',          'INTEGER', 'Number of header rows to skip'),
    ('csv.import.col.date',                        '0',          'INTEGER', 'Column index for transaction date'),
    ('csv.import.col.amount',                      '1',          'INTEGER', 'Column index for amount (negative = EXPENSE)'),
    ('csv.import.col.description',                 '2',          'INTEGER', 'Column index for description / communication'),
    ('csv.import.col.counterparty_name',           '3',          'INTEGER', 'Column index for counterparty name'),
    ('csv.import.col.counterparty_account',        '4',          'INTEGER', 'Column index for counterparty IBAN'),
    ('csv.import.col.external_reference',          '5',          'INTEGER', 'Column index for bank transaction reference'),
    ('csv.import.col.bank_account',                '6',          'INTEGER', 'Column index for own bank account IBAN'),
    ('csv.import.col.value_date',                  '-1',         'INTEGER', 'Column index for value date (-1 = absent)'),
    ('csv.import.suggestion.confidence.threshold', '3',          'INTEGER', 'Min confidence to show tag suggestion')
ON CONFLICT (config_key) DO NOTHING;

-- ─── seed: tag categories ────────────────────────────────────────────────────

INSERT INTO tag_category (name) VALUES
    ('Administration'), ('Consommables'), ('Dépôt'), ('Location'),
    ('Maintenance'), ('Prime'), ('Rente'), ('Taxes'), ('Travaux');

-- ─── seed: tag subcategories ─────────────────────────────────────────────────

INSERT INTO tag_subcategory (category_id, name, direction) VALUES
    ((SELECT id FROM tag_category WHERE name = 'Administration'), 'Assurance habitation', 'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Administration'), 'PEB',                  'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Administration'), 'Petits frais',         'BOTH'),
    ((SELECT id FROM tag_category WHERE name = 'Consommables'),   'Adoucisseur',          'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Consommables'),   'Eau',                  'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Dépôt'),         'Dépôt de garantie',    'INCOME'),
    ((SELECT id FROM tag_category WHERE name = 'Location'),       'Loyer',                'INCOME'),
    ((SELECT id FROM tag_category WHERE name = 'Location'),       'Charges',              'INCOME'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Chaudière',            'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Extincteur',           'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Maintenance'),    'Divers',               'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Taxes'),          'Précompte immobilier', 'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Travaux'),        'Rénovation',           'EXPENSE'),
    ((SELECT id FROM tag_category WHERE name = 'Travaux'),        'Entretien',            'EXPENSE');
