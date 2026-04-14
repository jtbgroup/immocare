-- ============================================================
-- V017 — UC016: Manage Estates (Phase 1)
-- Tables: estate, estate_member
-- Modified: app_user (role → is_platform_admin)
-- ============================================================

-- 1. Modify app_user: replace role column with is_platform_admin boolean
ALTER TABLE app_user DROP COLUMN role;
ALTER TABLE app_user ADD COLUMN is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE app_user SET is_platform_admin = TRUE WHERE username = 'admin';

-- 2. Create estate table (UUID primary key — non-guessable, multi-tenant safe)
CREATE TABLE estate (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  BIGINT       REFERENCES app_user(id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX idx_estate_name_ci ON estate (LOWER(name));

-- 3. Create estate_member join table
CREATE TABLE estate_member (
    estate_id  UUID        NOT NULL REFERENCES estate(id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL CHECK (role IN ('MANAGER', 'VIEWER')),
    added_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (estate_id, user_id)
);

CREATE INDEX idx_estate_member_estate ON estate_member(estate_id);
CREATE INDEX idx_estate_member_user   ON estate_member(user_id);
