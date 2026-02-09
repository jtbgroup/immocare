-- V001__create_users_table.sql
-- Create users table for authentication

CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_user_username ON app_user(username);
CREATE INDEX idx_user_email ON app_user(email);

-- Comments
COMMENT ON TABLE app_user IS 'System users with authentication credentials';
COMMENT ON COLUMN app_user.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN app_user.role IS 'User role: ADMIN (Phase 1 only)';
