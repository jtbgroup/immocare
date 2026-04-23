-- ============================================================
-- V001 — UC001: Authentication
-- Tables: app_user
-- ============================================================

CREATE TABLE app_user (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username            VARCHAR(50)  NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    email               VARCHAR(100) NOT NULL UNIQUE,
    is_platform_admin   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_user_username ON app_user (username);
CREATE INDEX idx_app_user_email    ON app_user (email);

-- Default admin — password: admin123 (BCrypt strength 12)
INSERT INTO app_user (username, password_hash, email, is_platform_admin)
VALUES (
    'admin',
    '$2b$12$Cjcy4.MrV8DCzBOQlLSdVuY5iKWeU7D1p7uGY0TqdK468LsB4v4vS',
    'admin@immocare.com',
    TRUE
);
