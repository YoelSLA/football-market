CREATE SEQUENCE player_image_sync_runs_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE player_image_sync_runs (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_image_sync_runs_id_seq'),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
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
