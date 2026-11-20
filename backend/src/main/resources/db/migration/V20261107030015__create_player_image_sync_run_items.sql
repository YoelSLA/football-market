CREATE SEQUENCE player_image_sync_run_items_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE player_image_sync_run_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_image_sync_run_items_id_seq'),
    run_id BIGINT NOT NULL REFERENCES player_image_sync_runs(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES players(id),
    previous_state VARCHAR(32) NOT NULL,
    final_state VARCHAR(32),
    result VARCHAR(32),
    identity_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    image_found_or_updated BOOLEAN NOT NULL DEFAULT FALSE,
    skip_reason VARCHAR(32),
    conflict BOOLEAN NOT NULL DEFAULT FALSE,
    error_occurred BOOLEAN NOT NULL DEFAULT FALSE,
    outcome_detail VARCHAR(512),
    finished_at TIMESTAMPTZ,
    CONSTRAINT uk_player_image_sync_run_items_run_player UNIQUE (run_id, player_id)
);

ALTER SEQUENCE player_image_sync_run_items_id_seq OWNED BY player_image_sync_run_items.id;
CREATE INDEX idx_player_image_sync_run_items_run_order
    ON player_image_sync_run_items(run_id, id);
