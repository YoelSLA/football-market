CREATE TABLE player_image_resolutions (
    player_id BIGINT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    last_attempt_at TIMESTAMPTZ
);

CREATE INDEX idx_player_image_resolutions_eligibility
    ON player_image_resolutions(status, last_attempt_at);
