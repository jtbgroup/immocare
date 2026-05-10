-- ============================================================
-- V001 — AUTH / UC007: Application users
-- ============================================================

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
    '$2b$12$Cjcy4.MrV8DCzBOQlLSdVuY5iKWeU7D1p7uGY0TqdK468LsB4v4vS',
    'admin@immocare.com',
    'ADMIN'
);
