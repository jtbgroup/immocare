-- Create user_account table
-- Note: 'user' is a reserved keyword in PostgreSQL, so we use 'user_account'

CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_user_username ON user_account(username);
CREATE INDEX idx_user_email ON user_account(email);

-- Add comments
COMMENT ON TABLE user_account IS 'Authentication and user management';
COMMENT ON COLUMN user_account.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN user_account.role IS 'User role (ADMIN for Phase 1)';
