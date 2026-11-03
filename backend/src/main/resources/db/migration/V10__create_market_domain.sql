-- Dominio de mercado de SPEC-010 (REQ-S2-06): saldos de crédito, tenencias de tokens, emisión
-- inicial por jugador y auditoría de compras.
--
-- Los importes se expresan en centésimas de crédito (CL-01): 100 centésimas equivalen a 1 crédito.
-- Un usuario normal recibe 100 créditos (10000 centésimas) y el superusuario 0.
--
-- No se modifica ninguna migración previa. La inicialización de usuarios y jugadores preexistentes
-- es idempotente: ignora los conflictos y nunca sobrescribe un saldo ya consumido ni repone tokens
-- ya vendidos, conforme a FR-015 y FR-016.

-- ----------------------------------------------------------------------------
-- user_credits — Saldo actual por usuario
-- ----------------------------------------------------------------------------
-- La presencia de la fila es un hecho persistente: una cuenta ausente durante una compra produce
-- rechazo por créditos insuficientes y no se crea en esa operación (FR-010).

CREATE TABLE user_credits (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    available_credits BIGINT NOT NULL DEFAULT 0 CHECK (available_credits >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Saldo inicial de los usuarios preexistentes. El superusuario arranca con 0 porque es el vendedor
-- del mercado y no compra. Ignorar conflictos conserva cualquier saldo ya establecido.
INSERT INTO user_credits (user_id, available_credits)
SELECT u.id,
       CASE WHEN u.role = 'SUPERUSER' THEN 0 ELSE 10000 END
FROM users u
ON CONFLICT (user_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- player_tokens — Tenencia actual por usuario y jugador
-- ----------------------------------------------------------------------------
-- Las filas con cantidad 0 son válidas: existen para simplificar los bloqueos posteriores. La
-- unicidad del par garantiza una sola tenencia por usuario y jugador.

CREATE TABLE player_tokens (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_player_tokens_player_owner UNIQUE (player_id, owner_user_id)
);

CREATE INDEX idx_player_tokens_player ON player_tokens(player_id);

-- ----------------------------------------------------------------------------
-- player_token_emissions — Emisión inicial única por jugador
-- ----------------------------------------------------------------------------
-- La cantidad de la emisión no es un parámetro: la restricción fija 100 de modo que PostgreSQL
-- rechace cualquier emisión con otra cantidad y la invariante no dependa solo del código.

CREATE TABLE player_token_emissions (
    player_id BIGINT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    initial_quantity INT NOT NULL CHECK (initial_quantity = 100),
    emitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Emisión inicial de los jugadores preexistentes, asignada al superusuario cuando existe. Sin
-- superusuario no hay propietario posible del inventario inicial, así que no se emite nada. Ignorar
-- conflictos impide una segunda emisión y, por tanto, reponer inventario ya vendido.
INSERT INTO player_token_emissions (player_id, owner_user_id, initial_quantity)
SELECT p.id, su.id, 100
FROM players p
CROSS JOIN (SELECT id FROM users WHERE role = 'SUPERUSER' ORDER BY id LIMIT 1) su
ON CONFLICT (player_id) DO NOTHING;

INSERT INTO player_tokens (player_id, owner_user_id, quantity)
SELECT e.player_id, e.owner_user_id, e.initial_quantity
FROM player_token_emissions e
ON CONFLICT (player_id, owner_user_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- market_transactions — Auditoría append-only de compras
-- ----------------------------------------------------------------------------
-- Sin cascadas a propósito: la auditoría histórica no se borra al eliminar un usuario o un jugador.
-- Los jugadores nunca se borran físicamente, se desactivan, de modo que estas filas se conservan.

CREATE TABLE market_transactions (
    id BIGSERIAL PRIMARY KEY,
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    seller_id BIGINT NOT NULL REFERENCES users(id),
    player_id BIGINT NOT NULL REFERENCES players(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price BIGINT NOT NULL CHECK (unit_price > 0),
    total_amount BIGINT NOT NULL CHECK (total_amount = quantity * unit_price),
    buyer_credits_before BIGINT NOT NULL CHECK (buyer_credits_before >= 0),
    buyer_credits_after BIGINT NOT NULL CHECK (buyer_credits_after >= 0),
    seller_credits_before BIGINT NOT NULL CHECK (seller_credits_before >= 0),
    seller_credits_after BIGINT NOT NULL CHECK (seller_credits_after >= 0),
    buyer_tokens_before INT NOT NULL CHECK (buyer_tokens_before >= 0),
    buyer_tokens_after INT NOT NULL CHECK (buyer_tokens_after >= 0),
    seller_tokens_before INT NOT NULL CHECK (seller_tokens_before >= 0),
    seller_tokens_after INT NOT NULL CHECK (seller_tokens_after >= 0),
    quote_calculated_at TIMESTAMPTZ NOT NULL,
    quote_strategy_version VARCHAR(64),
    executed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_market_transactions_buyer ON market_transactions(buyer_id, executed_at DESC, id DESC);
CREATE INDEX idx_market_transactions_seller ON market_transactions(seller_id, executed_at DESC, id DESC);