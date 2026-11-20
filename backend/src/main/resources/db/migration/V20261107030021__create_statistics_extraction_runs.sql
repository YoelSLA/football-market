CREATE SEQUENCE player_statistics_extraction_runs_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE player_statistics_extraction_runs (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_statistics_extraction_runs_id_seq'),
    extraction_date DATE NOT NULL,
    source VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    eligible_players BIGINT NOT NULL DEFAULT 0,
    excluded_without_reference BIGINT NOT NULL DEFAULT 0,
    selected_players BIGINT NOT NULL DEFAULT 0,
    completed_players BIGINT NOT NULL DEFAULT 0,
    no_data_players BIGINT NOT NULL DEFAULT 0,
    failed_players BIGINT NOT NULL DEFAULT 0,
    pending_players BIGINT NOT NULL DEFAULT 0,
    present_metrics BIGINT NOT NULL DEFAULT 0,
    absent_metrics BIGINT NOT NULL DEFAULT 0,
    not_derivable_metrics BIGINT NOT NULL DEFAULT 0,
    provider_calls BIGINT NOT NULL DEFAULT 0,
    failure_code VARCHAR(255),
    UNIQUE (id, extraction_date, source),
    CHECK (finished_at IS NULL OR finished_at >= started_at),
    CHECK (eligible_players >= 0 AND excluded_without_reference >= 0
        AND selected_players >= 0 AND completed_players >= 0
        AND no_data_players >= 0 AND failed_players >= 0 AND pending_players >= 0
        AND present_metrics >= 0 AND absent_metrics >= 0
        AND not_derivable_metrics >= 0 AND provider_calls >= 0)
);

ALTER SEQUENCE player_statistics_extraction_runs_id_seq
    OWNED BY player_statistics_extraction_runs.id;
CREATE UNIQUE INDEX uq_statistics_running_source
    ON player_statistics_extraction_runs(source) WHERE status = 'RUNNING';
CREATE INDEX ix_statistics_runs_campaign
    ON player_statistics_extraction_runs(extraction_date, source, started_at, id);
