-- V004: Create app_user table and insert default admin
-- Note: 'user' is a reserved keyword in PostgreSQL → use 'app_user'

CREATE TABLE app_user (
    id            BIGSERIAL     PRIMARY KEY,
    username      VARCHAR(50)   NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    email         VARCHAR(100)  NOT NULL UNIQUE,
    role          VARCHAR(20)   NOT NULL DEFAULT 'ADMIN',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_user_username ON app_user(username);
CREATE INDEX idx_app_user_email    ON app_user(email);

COMMENT ON TABLE  app_user                IS 'Authenticated users of ImmoCare';
COMMENT ON COLUMN app_user.password_hash  IS 'BCrypt(10) hashed password — never plain text';
COMMENT ON COLUMN app_user.role           IS 'ADMIN (Phase 1 only)';

-- Default admin — password: Admin1234!
-- Hash generated with BCrypt strength 10
INSERT INTO app_user (username, password_hash, email, role)
VALUES (
    'admin',
    '$2a$10$lw.L5djZidpIW/0Q8JoXlOhB8WZFCW9cSCwImJUGpIw6ooYQgZLIm',
    'admin@immocare.com',
    'ADMIN'
);
