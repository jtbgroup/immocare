-- Insert default admin user for development
-- Password: admin123 (hashed with BCrypt)

INSERT INTO user_account (username, password_hash, email, role, created_at, updated_at)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- admin123
    'admin@immocare.com',
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Add comment
COMMENT ON TABLE user_account IS 'Default admin user created for development (username: admin, password: admin123)';
