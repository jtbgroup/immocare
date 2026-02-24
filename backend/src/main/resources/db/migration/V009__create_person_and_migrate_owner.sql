-- ============================================================
-- V009: Create person table & migrate owner_name → owner_id FK
-- ============================================================

-- 1. Create person table
CREATE TABLE person (
    id             BIGSERIAL     PRIMARY KEY,
    last_name      VARCHAR(100)  NOT NULL,
    first_name     VARCHAR(100)  NOT NULL,
    birth_date     DATE          NULL,
    birth_place    VARCHAR(100)  NULL,
    national_id    VARCHAR(20)   NULL,
    gsm            VARCHAR(20)   NULL,
    email          VARCHAR(100)  NULL,
    street_address VARCHAR(200)  NULL,
    postal_code    VARCHAR(20)   NULL,
    city           VARCHAR(100)  NULL,
    country        VARCHAR(100)  NULL DEFAULT 'Belgium',
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_person_national_id UNIQUE (national_id)
);

-- 2. Insert one PERSON per distinct non-null owner_name in building
INSERT INTO person (last_name, first_name, country, created_at, updated_at)
SELECT DISTINCT
    CASE
        WHEN POSITION(' ' IN TRIM(owner_name)) > 0
            THEN TRIM(SUBSTRING(owner_name FROM POSITION(' ' IN TRIM(owner_name)) + 1))
        ELSE TRIM(owner_name)
    END AS last_name,
    CASE
        WHEN POSITION(' ' IN TRIM(owner_name)) > 0
            THEN TRIM(SUBSTRING(owner_name FROM 1 FOR POSITION(' ' IN TRIM(owner_name)) - 1))
        ELSE ''
    END AS first_name,
    'Belgium',
    NOW(),
    NOW()
FROM building
WHERE owner_name IS NOT NULL AND TRIM(owner_name) <> '';

-- 3. Insert PERSON per distinct non-null owner_name in housing_unit (not already existing)
INSERT INTO person (last_name, first_name, country, created_at, updated_at)
SELECT DISTINCT
    CASE
        WHEN POSITION(' ' IN TRIM(hu.owner_name)) > 0
            THEN TRIM(SUBSTRING(hu.owner_name FROM POSITION(' ' IN TRIM(hu.owner_name)) + 1))
        ELSE TRIM(hu.owner_name)
    END AS last_name,
    CASE
        WHEN POSITION(' ' IN TRIM(hu.owner_name)) > 0
            THEN TRIM(SUBSTRING(hu.owner_name FROM 1 FOR POSITION(' ' IN TRIM(hu.owner_name)) - 1))
        ELSE ''
    END AS first_name,
    'Belgium',
    NOW(),
    NOW()
FROM housing_unit hu
WHERE hu.owner_name IS NOT NULL
  AND TRIM(hu.owner_name) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM building b
      WHERE TRIM(b.owner_name) = TRIM(hu.owner_name)
        AND b.owner_name IS NOT NULL
  );

-- 4. Add owner_id FK columns to building and housing_unit
ALTER TABLE building
    ADD COLUMN owner_id BIGINT NULL REFERENCES person(id) ON DELETE SET NULL;

ALTER TABLE housing_unit
    ADD COLUMN owner_id BIGINT NULL REFERENCES person(id) ON DELETE SET NULL;

-- 5. Populate owner_id from matched person records
UPDATE building b
SET owner_id = p.id
FROM person p
WHERE b.owner_name IS NOT NULL
  AND TRIM(b.owner_name) <> ''
  AND (
      -- Match: "Firstname Lastname" stored as last_name=Lastname, first_name=Firstname
      LOWER(TRIM(b.owner_name)) = LOWER(TRIM(p.first_name) || ' ' || TRIM(p.last_name))
   OR LOWER(TRIM(b.owner_name)) = LOWER(TRIM(p.last_name) || ' ' || TRIM(p.first_name))
   OR LOWER(TRIM(b.owner_name)) = LOWER(TRIM(p.last_name))
  );

UPDATE housing_unit hu
SET owner_id = p.id
FROM person p
WHERE hu.owner_name IS NOT NULL
  AND TRIM(hu.owner_name) <> ''
  AND (
      LOWER(TRIM(hu.owner_name)) = LOWER(TRIM(p.first_name) || ' ' || TRIM(p.last_name))
   OR LOWER(TRIM(hu.owner_name)) = LOWER(TRIM(p.last_name) || ' ' || TRIM(p.first_name))
   OR LOWER(TRIM(hu.owner_name)) = LOWER(TRIM(p.last_name))
  );

-- owner_name columns kept for now; dropped in V010
