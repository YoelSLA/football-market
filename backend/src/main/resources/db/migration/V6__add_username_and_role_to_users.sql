-- Ampliación de SPEC-001: nombre de usuario y rol de usuario.
-- No se modifica ninguna migración histórica. Los usuarios existentes se preservan.

-- El nombre de usuario se deriva de forma determinista del email actual del usuario para no
-- generar valores aleatorios ni requerir intervención manual: se toma la parte local del email,
-- se normaliza a minúsculas y se filtran los caracteres admitidos por el formato ^[a-z0-9._-]+$.
-- El prefijo 'u' evita colisiones con nombres tomados de emails y garantiza longitud mínima (>= 3).
-- Si aun así quedara una colisión (parte local completamente filtrada, emails repetidos con
-- distinta capitalización), se completa con el identificador del usuario.
ALTER TABLE users ADD COLUMN username VARCHAR(30);
ALTER TABLE users ADD COLUMN role VARCHAR(255);

UPDATE users
SET username = 'u_' || COALESCE(NULLIF(
    NULLIF(
        regexp_replace(lower(split_part(email, '@', 1)), '[^a-z0-9._-]', '', 'g'),
        ''
    ),
    NULL
), 'user')
WHERE username IS NULL;

UPDATE users
SET username = 'u_' || COALESCE(NULLIF(
    NULLIF(
        regexp_replace(lower(split_part(email, '@', 1)), '[^a-z0-9._-]', '', 'g'),
        ''
    ),
    NULL
), 'user') || '_' || id
WHERE username IN (
    SELECT username FROM (
        SELECT username, row_number() OVER (PARTITION BY username ORDER BY id) AS rn
        FROM users
    ) duplicados
    WHERE rn > 1
);

-- Todo usuario existente es un usuario registrado públicamente antes de que existiera el rol.
UPDATE users SET role = 'USER' WHERE role IS NULL;

-- Los usuarios existentes con nombre de usuario muy largo se recortan de forma determinista.
UPDATE users SET username = left(username, 24) || '_' || id WHERE length(username) > 30;

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN role SET NOT NULL;

ALTER TABLE users ADD CONSTRAINT users_username_unique UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT users_username_format CHECK (username ~ '^[a-z0-9._-]{3,30}$');
ALTER TABLE users ADD CONSTRAINT users_role_allowed CHECK (role IN ('USER', 'SUPERUSER'));

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);