CREATE TABLE market_transactions (
    id BIGSERIAL PRIMARY KEY,
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    seller_id BIGINT NOT NULL REFERENCES users(id),
    player_id BIGINT NOT NULL REFERENCES players(id),
    listing_id BIGINT REFERENCES token_listings(id) ON DELETE NO ACTION,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price BIGINT NOT NULL CHECK (unit_price > 0),
    total_amount BIGINT NOT NULL CHECK (total_amount = quantity * unit_price),
    buyer_credits_before BIGINT NOT NULL CHECK (buyer_credits_before >= 0),
    buyer_credits_after BIGINT NOT NULL CHECK (buyer_credits_after >= 0),
    seller_credits_before BIGINT NOT NULL CHECK (seller_credits_before >= 0),
    seller_credits_after BIGINT NOT NULL CHECK (seller_credits_after >= 0),
    buyer_tokens_before INTEGER NOT NULL CHECK (buyer_tokens_before >= 0),
    buyer_tokens_after INTEGER NOT NULL CHECK (buyer_tokens_after >= 0),
    seller_tokens_before INTEGER NOT NULL CHECK (seller_tokens_before >= 0),
    seller_tokens_after INTEGER NOT NULL CHECK (seller_tokens_after >= 0),
    quote_calculated_at TIMESTAMPTZ NOT NULL,
    quote_strategy_version VARCHAR(64),
    executed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_market_transactions_buyer
    ON market_transactions(buyer_id, executed_at DESC, id DESC);
CREATE INDEX idx_market_transactions_seller
    ON market_transactions(seller_id, executed_at DESC, id DESC);
