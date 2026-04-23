-- ============================================================
-- V004 — UC005: Manage Buildings
-- Tables: building
-- ============================================================

CREATE TABLE building (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    street_address VARCHAR(200) NOT NULL,
    postal_code    VARCHAR(20)  NOT NULL,
    city           VARCHAR(100) NOT NULL,
    country        VARCHAR(100) NOT NULL DEFAULT 'Belgium',
    owner_id       BIGINT       NULL REFERENCES person (id) ON DELETE SET NULL,
    estate_id      UUID NOT NULL REFERENCES estate(id),
    created_by     BIGINT       NULL REFERENCES app_user (id) ON DELETE SET NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_building_city       ON building (city);
CREATE INDEX idx_building_owner      ON building (owner_id);
CREATE INDEX idx_building_created_by ON building (created_by);
