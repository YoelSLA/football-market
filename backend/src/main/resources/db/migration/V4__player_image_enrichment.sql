ALTER TABLE players ADD COLUMN fallback_image_url VARCHAR(2048);

CREATE TABLE player_image_resolutions (
    player_id BIGINT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    last_attempt_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_player_image_resolutions_eligibility
    ON player_image_resolutions(status, last_attempt_at);
INSERT INTO player_image_resolutions (player_id, status)
SELECT id, 'PENDING' FROM players;

CREATE SEQUENCE player_image_sync_runs_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_image_sync_runs (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_image_sync_runs_id_seq'),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    force BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255),
    evaluated INTEGER NOT NULL DEFAULT 0,
    processed INTEGER NOT NULL DEFAULT 0,
    found INTEGER NOT NULL DEFAULT 0,
    not_found INTEGER NOT NULL DEFAULT 0,
    retryable_errors INTEGER NOT NULL DEFAULT 0,
    failed INTEGER NOT NULL DEFAULT 0,
    conflicts INTEGER NOT NULL DEFAULT 0,
    skipped_found INTEGER NOT NULL DEFAULT 0,
    skipped_retry_window INTEGER NOT NULL DEFAULT 0,
    skipped_failed INTEGER NOT NULL DEFAULT 0,
    interrupted INTEGER NOT NULL DEFAULT 0
);
ALTER SEQUENCE player_image_sync_runs_id_seq OWNED BY player_image_sync_runs.id;
CREATE INDEX idx_player_image_sync_runs_history
    ON player_image_sync_runs(started_at DESC, id DESC);
CREATE INDEX idx_player_image_sync_runs_status ON player_image_sync_runs(status);

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
    finished_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_player_image_sync_run_items_run_player UNIQUE (run_id, player_id)
);
ALTER SEQUENCE player_image_sync_run_items_id_seq OWNED BY player_image_sync_run_items.id;
CREATE INDEX idx_player_image_sync_run_items_run_order
    ON player_image_sync_run_items(run_id, id);
