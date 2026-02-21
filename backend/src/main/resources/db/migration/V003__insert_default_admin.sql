-- V003: Insert default admin user
-- Password: admin123
-- Hash generated with BCryptPasswordEncoder strength 12

INSERT INTO app_user (username, password_hash, email, role)
VALUES (
    'admin',
    '$2a$12$5z2u7D4w9NIJbFRJ/g9/A.w3SWPX01nOyifIfuo.09HsNLkBRUiCy',
    'admin@immocare.com',
    'ADMIN'
);
