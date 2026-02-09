-- V008__insert_default_admin.sql
-- Insert default admin user

-- Password: Admin123!
-- BCrypt hash generated with strength 10
INSERT INTO app_user (username, password_hash, email, role)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin@immocare.local',
    'ADMIN'
);

-- Comments
COMMENT ON TABLE app_user IS 'Default admin user created. Username: admin, Password: Admin123! - CHANGE IN PRODUCTION!';
