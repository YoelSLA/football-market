-- index_player_tokens_owner
CREATE INDEX idx_player_tokens_owner ON player_tokens (owner_user_id, player_id);
