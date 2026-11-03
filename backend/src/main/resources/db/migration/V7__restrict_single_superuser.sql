-- El SUPERUSER es una cuenta singular del dominio de mercado: el dueño inicial de la totalidad
-- de los tokens. La unicidad se garantiza en la aplicación mediante una provisión idempotente, y
-- este índice la vuelve además una invariante del dato, independiente del código de aplicación.
CREATE UNIQUE INDEX idx_users_single_superuser ON users ((TRUE)) WHERE role = 'SUPERUSER';