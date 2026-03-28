-- ============================================================
-- V002 — UC009: Persons
-- ============================================================

CREATE TABLE person (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
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
