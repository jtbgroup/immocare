-- ============================================================
-- V001: Initial schema — full baseline
-- Consolidates V001–V014 into a single migration.
-- ============================================================

-- ─── app_user ────────────────────────────────────────────────────────────────

CREATE TABLE app_user (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_user_username ON app_user (username);
CREATE INDEX idx_app_user_email    ON app_user (email);

COMMENT ON TABLE  app_user               IS 'Authenticated users of ImmoCare';
COMMENT ON COLUMN app_user.password_hash IS 'BCrypt(12) hashed password — never plain text';
COMMENT ON COLUMN app_user.role          IS 'ADMIN (Phase 1 only)';

-- Default admin — password: admin123 (BCrypt strength 12)
INSERT INTO app_user (username, password_hash, email, role)
VALUES (
    'admin',
    '$2a$12$5z2u7D4w9NIJbFRJ/g9/A.w3SWPX01nOyifIfuo.09HsNLkBRUiCy',
    'admin@immocare.com',
    'ADMIN'
);

-- ─── person ──────────────────────────────────────────────────────────────────

CREATE TABLE person (
    id             BIGSERIAL    PRIMARY KEY,
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

-- ─── building ────────────────────────────────────────────────────────────────

CREATE TABLE building (
    id             BIGSERIAL    PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    street_address VARCHAR(200) NOT NULL,
    postal_code    VARCHAR(20)  NOT NULL,
    city           VARCHAR(100) NOT NULL,
    country        VARCHAR(100) NOT NULL DEFAULT 'Belgium',
    owner_id       BIGINT       NULL REFERENCES person (id) ON DELETE SET NULL,
    created_by     BIGINT       NULL REFERENCES app_user (id) ON DELETE SET NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_building_created_by ON building (created_by);
CREATE INDEX idx_building_city       ON building (city);
CREATE INDEX idx_building_owner      ON building (owner_id);

COMMENT ON TABLE  building        IS 'Physical buildings containing housing units';
COMMENT ON COLUMN building.name   IS 'Building name or identifier';
COMMENT ON COLUMN building.owner_id IS 'FK to person — building owner';

-- ─── housing_unit ────────────────────────────────────────────────────────────

CREATE TABLE housing_unit (
    id                   BIGSERIAL     PRIMARY KEY,
    building_id          BIGINT        NOT NULL REFERENCES building (id) ON DELETE CASCADE,
    unit_number          VARCHAR(20)   NOT NULL,
    floor                INTEGER       NOT NULL,
    landing_number       VARCHAR(10)   NULL,
    total_surface        DECIMAL(7,2)  NULL CHECK (total_surface > 0),
    has_terrace          BOOLEAN       NOT NULL DEFAULT FALSE,
    terrace_surface      DECIMAL(7,2)  NULL CHECK (terrace_surface > 0),
    terrace_orientation  VARCHAR(2)    NULL,
    has_garden           BOOLEAN       NOT NULL DEFAULT FALSE,
    garden_surface       DECIMAL(7,2)  NULL CHECK (garden_surface > 0),
    garden_orientation   VARCHAR(2)    NULL,
    owner_id             BIGINT        NULL REFERENCES person (id) ON DELETE SET NULL,
    created_by           BIGINT        NULL REFERENCES app_user (id) ON DELETE SET NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_housing_unit_number    UNIQUE (building_id, unit_number),
    CONSTRAINT chk_floor                 CHECK (floor BETWEEN -10 AND 100),
    CONSTRAINT chk_terrace_orientation   CHECK (terrace_orientation IN ('N','S','E','W','NE','NW','SE','SW')),
    CONSTRAINT chk_garden_orientation    CHECK (garden_orientation  IN ('N','S','E','W','NE','NW','SE','SW'))
);

CREATE INDEX idx_housing_unit_building_id ON housing_unit (building_id);
CREATE INDEX idx_housing_unit_created_by  ON housing_unit (created_by);
CREATE INDEX idx_housing_unit_owner       ON housing_unit (owner_id);

COMMENT ON TABLE  housing_unit                    IS 'Individual apartments / units within a building';
COMMENT ON COLUMN housing_unit.unit_number        IS 'Unit identifier, unique within a building (e.g. A101, 1B)';
COMMENT ON COLUMN housing_unit.floor              IS 'Floor number; negative values indicate underground levels';
COMMENT ON COLUMN housing_unit.total_surface      IS 'Total surface in m²; may be entered manually or derived from rooms';
COMMENT ON COLUMN housing_unit.terrace_orientation IS 'Cardinal orientation of the terrace (N,S,E,W,NE,NW,SE,SW)';
COMMENT ON COLUMN housing_unit.garden_orientation  IS 'Cardinal orientation of the garden (N,S,E,W,NE,NW,SE,SW)';
COMMENT ON COLUMN housing_unit.owner_id            IS 'Unit-specific owner; overrides building.owner_id when set';

-- ─── rent_history ────────────────────────────────────────────────────────────

CREATE TABLE rent_history (
    id              BIGSERIAL     PRIMARY KEY,
    housing_unit_id BIGINT        NOT NULL REFERENCES housing_unit (id) ON DELETE CASCADE,
    monthly_rent    NUMERIC(10,2) NOT NULL CHECK (monthly_rent > 0),
    effective_from  DATE          NOT NULL,
    effective_to    DATE          NULL,
    notes           VARCHAR(500)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- Only one active (current) rent per unit
    CONSTRAINT uq_rent_current UNIQUE (housing_unit_id, effective_to) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_rent_history_unit_id        ON rent_history (housing_unit_id);
CREATE INDEX idx_rent_history_effective_from ON rent_history (housing_unit_id, effective_from DESC);

-- ─── room ────────────────────────────────────────────────────────────────────

CREATE TABLE room (
    id                  BIGSERIAL    PRIMARY KEY,
    housing_unit_id     BIGINT       NOT NULL REFERENCES housing_unit (id) ON DELETE CASCADE,
    room_type           VARCHAR(20)  NOT NULL,
    approximate_surface DECIMAL(6,2) NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_room_approximate_surface CHECK (approximate_surface > 0 AND approximate_surface < 1000),
    CONSTRAINT chk_room_type CHECK (room_type IN (
        'LIVING_ROOM','BEDROOM','KITCHEN','BATHROOM',
        'TOILET','HALLWAY','STORAGE','OFFICE','DINING_ROOM','OTHER'
    ))
);

CREATE INDEX idx_room_housing_unit_id ON room (housing_unit_id);

COMMENT ON TABLE  room                     IS 'Individual rooms within a housing unit';
COMMENT ON COLUMN room.room_type           IS 'Enum: LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM, TOILET, HALLWAY, STORAGE, OFFICE, DINING_ROOM, OTHER';
COMMENT ON COLUMN room.approximate_surface IS 'Approximate surface in m² (not legally binding); must be > 0 and < 1000';

-- ─── peb_score_history ───────────────────────────────────────────────────────

CREATE TABLE peb_score_history (
    id                 BIGSERIAL   PRIMARY KEY,
    housing_unit_id    BIGINT      NOT NULL REFERENCES housing_unit (id) ON DELETE CASCADE,
    peb_score          VARCHAR(10) NOT NULL,
    score_date         DATE        NOT NULL,
    certificate_number VARCHAR(50) NULL,
    valid_until        DATE        NULL,
    created_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_peb_score_value CHECK (peb_score IN ('A_PLUS_PLUS','A_PLUS','A','B','C','D','E','F','G'))
);

CREATE INDEX idx_peb_score_unit_date     ON peb_score_history (housing_unit_id, score_date DESC);
CREATE INDEX idx_peb_score_unit_validity ON peb_score_history (housing_unit_id, valid_until);

COMMENT ON TABLE  peb_score_history                    IS 'Append-only PEB energy certificate history per housing unit';
COMMENT ON COLUMN peb_score_history.peb_score          IS 'Enum: A_PLUS_PLUS, A_PLUS, A, B, C, D, E, F, G';
COMMENT ON COLUMN peb_score_history.score_date         IS 'Date of certificate issuance; cannot be in the future';
COMMENT ON COLUMN peb_score_history.certificate_number IS 'Optional certificate reference number';
COMMENT ON COLUMN peb_score_history.valid_until        IS 'Optional expiry date; must be after score_date if set';

-- ─── meter ───────────────────────────────────────────────────────────────────

CREATE TABLE meter (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type                VARCHAR(20)  NOT NULL,
    meter_number        VARCHAR(50)  NOT NULL,
    label               VARCHAR(100) NULL,
    ean_code            VARCHAR(18)  NULL,
    installation_number VARCHAR(50)  NULL,
    customer_number     VARCHAR(50)  NULL,
    owner_type          VARCHAR(20)  NOT NULL,
    owner_id            BIGINT       NOT NULL,
    start_date          DATE         NOT NULL,
    end_date            DATE         NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_meter_type           CHECK (type       IN ('WATER','GAS','ELECTRICITY')),
    CONSTRAINT chk_meter_owner_type     CHECK (owner_type IN ('HOUSING_UNIT','BUILDING')),
    CONSTRAINT chk_meter_end_after_start CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Active meter queries (most frequent)
CREATE INDEX idx_meter_owner_active  ON meter (owner_type, owner_id) WHERE end_date IS NULL;
-- History queries
CREATE INDEX idx_meter_owner_history ON meter (owner_type, owner_id, start_date DESC);

COMMENT ON TABLE  meter                     IS 'Append-only utility meter history (WATER, GAS, ELECTRICITY) per housing unit or building';
COMMENT ON COLUMN meter.type                IS 'Enum: WATER, GAS, ELECTRICITY';
COMMENT ON COLUMN meter.meter_number        IS 'Physical meter identifier';
COMMENT ON COLUMN meter.label               IS 'Optional human-readable label (e.g. Kitchen, Basement)';
COMMENT ON COLUMN meter.ean_code            IS 'Required for GAS and ELECTRICITY meters';
COMMENT ON COLUMN meter.installation_number IS 'Required for WATER meters';
COMMENT ON COLUMN meter.customer_number     IS 'Required for WATER meters on a BUILDING';
COMMENT ON COLUMN meter.owner_type          IS 'Enum: HOUSING_UNIT, BUILDING';
COMMENT ON COLUMN meter.owner_id            IS 'FK to housing_unit.id or building.id depending on owner_type';
COMMENT ON COLUMN meter.start_date          IS 'Activation date — cannot be in the future';
COMMENT ON COLUMN meter.end_date            IS 'Closure date — NULL means active';

-- ─── lease ───────────────────────────────────────────────────────────────────

CREATE TABLE lease (
    id                           BIGSERIAL     PRIMARY KEY,
    housing_unit_id              BIGINT        NOT NULL REFERENCES housing_unit (id) ON DELETE RESTRICT,
    status                       VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    signature_date               DATE          NOT NULL,
    start_date                   DATE          NOT NULL,
    end_date                     DATE          NOT NULL,
    lease_type                   VARCHAR(30)   NOT NULL,
    duration_months              INTEGER       NOT NULL CHECK (duration_months > 0),
    notice_period_months         INTEGER       NOT NULL CHECK (notice_period_months > 0),
    monthly_rent                 NUMERIC(10,2) NOT NULL CHECK (monthly_rent > 0),
    monthly_charges              NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (monthly_charges >= 0),
    charges_type                 VARCHAR(20)   NOT NULL DEFAULT 'FORFAIT',
    charges_description          TEXT          NULL,
    registration_spf             VARCHAR(50)   NULL,
    registration_region          VARCHAR(50)   NULL,
    registration_inventory_spf   VARCHAR(100)  NULL,
    registration_inventory_region VARCHAR(100) NULL,
    deposit_amount               NUMERIC(10,2) NULL CHECK (deposit_amount >= 0),
    deposit_type                 VARCHAR(30)   NULL,
    deposit_reference            VARCHAR(100)  NULL,
    tenant_insurance_confirmed   BOOLEAN       NOT NULL DEFAULT FALSE,
    tenant_insurance_reference   VARCHAR(100)  NULL,
    tenant_insurance_expiry      DATE          NULL,
    created_at                   TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_lease_status     CHECK (status     IN ('DRAFT','ACTIVE','FINISHED','CANCELLED')),
    CONSTRAINT chk_lease_type       CHECK (lease_type IN ('SHORT_TERM','MAIN_RESIDENCE_3Y','MAIN_RESIDENCE_6Y','MAIN_RESIDENCE_9Y','STUDENT','GLIDING','COMMERCIAL')),
    CONSTRAINT chk_charges_type     CHECK (charges_type IN ('FORFAIT','PROVISION')),
    CONSTRAINT chk_deposit_type     CHECK (deposit_type IN ('BLOCKED_ACCOUNT','BANK_GUARANTEE','CPAS','INSURANCE') OR deposit_type IS NULL),
    CONSTRAINT chk_end_after_start  CHECK (end_date >= start_date)
);

-- Prevent multiple ACTIVE or DRAFT leases on the same unit
CREATE UNIQUE INDEX uq_lease_unit_active_draft ON lease (housing_unit_id) WHERE status IN ('ACTIVE','DRAFT');
CREATE INDEX idx_lease_housing_unit ON lease (housing_unit_id);
CREATE INDEX idx_lease_status       ON lease (status);

-- ─── lease_tenant ────────────────────────────────────────────────────────────

CREATE TABLE lease_tenant (
    lease_id  BIGINT      NOT NULL REFERENCES lease  (id) ON DELETE CASCADE,
    person_id BIGINT      NOT NULL REFERENCES person (id) ON DELETE RESTRICT,
    role      VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',
    CONSTRAINT pk_lease_tenant  PRIMARY KEY (lease_id, person_id),
    CONSTRAINT chk_tenant_role  CHECK (role IN ('PRIMARY','CO_TENANT','GUARANTOR'))
);

CREATE INDEX idx_lease_tenant_person ON lease_tenant (person_id);

-- ─── lease_rent_adjustment ───────────────────────────────────────────────────

CREATE TABLE lease_rent_adjustment (
    id             BIGSERIAL     PRIMARY KEY,
    lease_id       BIGINT        NOT NULL REFERENCES lease (id) ON DELETE CASCADE,
    field          VARCHAR(10)   NOT NULL,
    old_value      NUMERIC(10,2) NOT NULL,
    new_value      NUMERIC(10,2) NOT NULL,
    reason         TEXT          NOT NULL,
    effective_date DATE          NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_rent_adj_field CHECK (field IN ('RENT','CHARGES'))
);

CREATE INDEX idx_rent_adj_lease ON lease_rent_adjustment (lease_id, effective_date DESC);
