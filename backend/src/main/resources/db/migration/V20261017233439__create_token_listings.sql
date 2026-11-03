CREATE TABLE token_listings (
    id BIGSERIAL PRIMARY KEY,
    seller_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE NO ACTION,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE NO ACTION,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price BIGINT NOT NULL CHECK (unit_price > 0),
    total_amount BIGINT NOT NULL CHECK (total_amount = quantity * unit_price),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SOLD', 'CANCELLED')),
    quote_calculated_at TIMESTAMPTZ NOT NULL,
    quote_strategy_version VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at > created_at),
    closed_at TIMESTAMPTZ
);

CREATE INDEX idx_token_listings_active ON token_listings (expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_token_listings_seller ON token_listings (seller_user_id, created_at DESC);
