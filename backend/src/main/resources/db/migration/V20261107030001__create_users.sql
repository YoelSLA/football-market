CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(30) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    CONSTRAINT users_username_format CHECK (username ~ '^[a-z0-9._-]{3,30}$'),
    CONSTRAINT users_role_allowed CHECK (role IN ('USER', 'SUPERUSER'))
);

CREATE INDEX idx_users_role ON users(role);
CREATE UNIQUE INDEX idx_users_single_superuser ON users ((TRUE)) WHERE role = 'SUPERUSER';
