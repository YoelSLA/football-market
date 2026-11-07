CREATE TABLE player_tokens (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_player_tokens_player_owner UNIQUE (player_id, owner_user_id)
);

CREATE INDEX idx_player_tokens_player ON player_tokens(player_id);
CREATE INDEX idx_player_tokens_owner ON player_tokens(owner_user_id, player_id);
