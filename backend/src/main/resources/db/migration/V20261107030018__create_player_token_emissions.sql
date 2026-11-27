CREATE TABLE player_token_emissions (
    player_id BIGINT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    initial_quantity INTEGER NOT NULL CHECK (initial_quantity = 100),
    emitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_player_token_emissions_owner ON player_token_emissions(owner_user_id);
