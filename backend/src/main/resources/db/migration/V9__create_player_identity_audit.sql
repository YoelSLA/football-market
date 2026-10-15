CREATE UNIQUE INDEX uk_player_external_references_whoscored_player
    ON player_external_references (player_id) WHERE provider = 'WHOSCORED';

CREATE SEQUENCE player_identity_sync_runs_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_identity_sync_runs (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_identity_sync_runs_id_seq'),
    pass_type VARCHAR(255) NOT NULL,
    source_run_id BIGINT REFERENCES player_identity_sync_runs(id),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(255) NOT NULL,
    failure_reason VARCHAR(255),
    counters JSONB,
    CHECK ((pass_type = 'CURRENT' AND source_run_id IS NULL)
        OR (pass_type = 'HISTORICAL' AND source_run_id IS NOT NULL)),
    CHECK (finished_at IS NULL OR finished_at >= started_at)
);
CREATE INDEX ix_identity_runs_started ON player_identity_sync_runs(started_at, id);
CREATE INDEX ix_identity_runs_source ON player_identity_sync_runs(source_run_id);

CREATE SEQUENCE player_identity_league_discoveries_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_identity_league_discoveries (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_identity_league_discoveries_id_seq'),
    run_id BIGINT NOT NULL REFERENCES player_identity_sync_runs(id),
    league_id BIGINT NOT NULL,
    league_name VARCHAR(255) NOT NULL,
    tournament_id INTEGER NOT NULL,
    status VARCHAR(255) NOT NULL,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    pages_read BIGINT NOT NULL,
    last_page INTEGER,
    total_pages INTEGER,
    total_results BIGINT,
    rows_read BIGINT NOT NULL,
    discarded_rows BIGINT NOT NULL,
    error_code VARCHAR(255),
    error_message VARCHAR(255),
    failed_page INTEGER,
    http_status INTEGER,
    UNIQUE(run_id, league_id),
    UNIQUE(run_id, id),
    CHECK (pages_read >= 0 AND rows_read >= 0 AND discarded_rows >= 0 AND discarded_rows <= rows_read)
);

CREATE SEQUENCE whoscored_identity_candidates_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE whoscored_identity_candidates (
    id BIGINT PRIMARY KEY DEFAULT nextval('whoscored_identity_candidates_id_seq'),
    run_id BIGINT NOT NULL REFERENCES player_identity_sync_runs(id),
    external_id VARCHAR(255) NOT NULL,
    contexts JSONB NOT NULL,
    UNIQUE(run_id, external_id)
);

CREATE SEQUENCE player_identity_sync_items_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_identity_sync_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_identity_sync_items_id_seq'),
    run_id BIGINT NOT NULL REFERENCES player_identity_sync_runs(id),
    player_id BIGINT NOT NULL,
    player_name VARCHAR(255),
    team_id BIGINT,
    team_name VARCHAR(255),
    league_id BIGINT,
    league_name VARCHAR(255),
    initial_external_id VARCHAR(255),
    initial_external_ids JSONB NOT NULL,
    result VARCHAR(255) NOT NULL,
    reason_code VARCHAR(255),
    reason_message VARCHAR(255),
    candidate_external_ids JSONB NOT NULL,
    confirmed_external_id VARCHAR(255),
    decided_at TIMESTAMPTZ,
    UNIQUE(run_id, player_id)
);
CREATE INDEX ix_identity_items_result ON player_identity_sync_items(run_id, result, id);

CREATE SEQUENCE player_identity_discarded_rows_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_identity_discarded_rows (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_identity_discarded_rows_id_seq'),
    run_id BIGINT NOT NULL REFERENCES player_identity_sync_runs(id),
    discovery_id BIGINT NOT NULL,
    page INTEGER NOT NULL CHECK (page > 0),
    row_position INTEGER NOT NULL CHECK (row_position > 0),
    reason_code VARCHAR(255) NOT NULL,
    FOREIGN KEY (run_id, discovery_id) REFERENCES player_identity_league_discoveries(run_id, id)
);
CREATE INDEX ix_identity_discards_run ON player_identity_discarded_rows(run_id);
