-- ============================================================
-- V011: Create lease, lease_tenant, lease_indexation_history
-- ============================================================

CREATE TABLE lease (
    id                            BIGSERIAL     PRIMARY KEY,
    housing_unit_id               BIGINT        NOT NULL REFERENCES housing_unit(id) ON DELETE RESTRICT,
    status                        VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    signature_date                DATE          NOT NULL,
    start_date                    DATE          NOT NULL,
    end_date                      DATE          NOT NULL,
    lease_type                    VARCHAR(30)   NOT NULL,
    duration_months               INTEGER       NOT NULL CHECK (duration_months > 0),
    notice_period_months          INTEGER       NOT NULL CHECK (notice_period_months > 0),
    indexation_notice_days        INTEGER       NOT NULL DEFAULT 30,
    indexation_anniversary_month  INTEGER       NULL     CHECK (indexation_anniversary_month BETWEEN 1 AND 12),
    monthly_rent                  NUMERIC(10,2) NOT NULL CHECK (monthly_rent > 0),
    monthly_charges               NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (monthly_charges >= 0),
    charges_type                  VARCHAR(20)   NOT NULL DEFAULT 'FORFAIT',
    charges_description           TEXT          NULL,
    base_index_value              NUMERIC(8,4)  NULL,
    base_index_month              DATE          NULL,
    registration_spf              VARCHAR(50)   NULL,
    registration_region           VARCHAR(50)   NULL,
    deposit_amount                NUMERIC(10,2) NULL     CHECK (deposit_amount >= 0),
    deposit_type                  VARCHAR(30)   NULL,
    deposit_reference             VARCHAR(100)  NULL,
    tenant_insurance_confirmed    BOOLEAN       NOT NULL DEFAULT FALSE,
    tenant_insurance_reference    VARCHAR(100)  NULL,
    tenant_insurance_expiry       DATE          NULL,
    created_at                    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_lease_status   CHECK (status IN ('DRAFT','ACTIVE','FINISHED','CANCELLED')),
    CONSTRAINT chk_lease_type     CHECK (lease_type IN ('SHORT_TERM','MAIN_RESIDENCE_3Y','MAIN_RESIDENCE_6Y','MAIN_RESIDENCE_9Y','STUDENT','GLIDING','COMMERCIAL')),
    CONSTRAINT chk_charges_type   CHECK (charges_type IN ('FORFAIT','PROVISION')),
    CONSTRAINT chk_deposit_type   CHECK (deposit_type IN ('BLOCKED_ACCOUNT','BANK_GUARANTEE','CPAS','INSURANCE') OR deposit_type IS NULL),
    CONSTRAINT chk_end_after_start CHECK (end_date >= start_date)
);

-- Prevent multiple ACTIVE or DRAFT leases on the same unit
CREATE UNIQUE INDEX uq_lease_unit_active_draft ON lease (housing_unit_id) WHERE status IN ('ACTIVE','DRAFT');
CREATE INDEX idx_lease_housing_unit ON lease (housing_unit_id);
CREATE INDEX idx_lease_status       ON lease (status);

CREATE TABLE lease_tenant (
    lease_id  BIGINT      NOT NULL REFERENCES lease(id)  ON DELETE CASCADE,
    person_id BIGINT      NOT NULL REFERENCES person(id) ON DELETE RESTRICT,
    role      VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',
    CONSTRAINT pk_lease_tenant PRIMARY KEY (lease_id, person_id),
    CONSTRAINT chk_tenant_role CHECK (role IN ('PRIMARY','CO_TENANT','GUARANTOR'))
);
CREATE INDEX idx_lease_tenant_person ON lease_tenant (person_id);

CREATE TABLE lease_indexation_history (
    id                BIGSERIAL     PRIMARY KEY,
    lease_id          BIGINT        NOT NULL REFERENCES lease(id) ON DELETE CASCADE,
    application_date  DATE          NOT NULL,
    old_rent          NUMERIC(10,2) NOT NULL,
    new_index_value   NUMERIC(8,4)  NOT NULL,
    new_index_month   DATE          NOT NULL,
    applied_rent      NUMERIC(10,2) NOT NULL,
    notification_date DATE          NULL,
    notes             TEXT          NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_indexation_lease ON lease_indexation_history (lease_id);
