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
    max_players INTEGER,
    UNIQUE (id, extraction_date, source),
    CHECK (finished_at IS NULL OR finished_at >= started_at),
    CHECK (max_players IS NULL OR max_players > 0),
    CHECK (source <> 'JSON' OR max_players IS NULL),
    CHECK (eligible_players >= 0 AND excluded_without_reference >= 0
        AND selected_players >= 0 AND completed_players >= 0
        AND no_data_players >= 0 AND failed_players >= 0 AND pending_players >= 0
        AND present_metrics >= 0 AND absent_metrics >= 0
        AND not_derivable_metrics >= 0 AND provider_calls >= 0),
    CHECK (max_players IS NULL OR selected_players <= max_players)
);
CREATE UNIQUE INDEX uq_statistics_running_source
    ON player_statistics_extraction_runs (source) WHERE status = 'RUNNING';
CREATE INDEX ix_statistics_runs_campaign
    ON player_statistics_extraction_runs (extraction_date, source, started_at, id);

CREATE SEQUENCE player_statistics_run_items_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_statistics_run_items (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_statistics_run_items_id_seq'),
    run_id BIGINT NOT NULL,
    extraction_date DATE NOT NULL,
    source VARCHAR(32) NOT NULL,
    player_id BIGINT NOT NULL REFERENCES players(id),
    provider_player_id VARCHAR(255) NOT NULL,
    result VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    contexts_count INTEGER NOT NULL DEFAULT 0,
    present_metrics INTEGER NOT NULL DEFAULT 0,
    absent_metrics INTEGER NOT NULL DEFAULT 0,
    incident_code VARCHAR(255),
    FOREIGN KEY (run_id, extraction_date, source)
        REFERENCES player_statistics_extraction_runs(id, extraction_date, source),
    CHECK (finished_at IS NULL OR finished_at >= started_at),
    CHECK (contexts_count >= 0 AND present_metrics >= 0 AND absent_metrics >= 0)
);
CREATE INDEX ix_statistics_items_run_result
    ON player_statistics_run_items (run_id, result, id);
CREATE INDEX ix_statistics_items_campaign_player
    ON player_statistics_run_items (extraction_date, source, player_id);
CREATE UNIQUE INDEX uq_statistics_item_completed_campaign
    ON player_statistics_run_items (extraction_date, source, player_id)
    WHERE result = 'COMPLETED';

CREATE SEQUENCE player_performance_contexts_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_performance_contexts (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_performance_contexts_id_seq'),
    run_item_id BIGINT NOT NULL REFERENCES player_statistics_run_items(id),
    provider_competition_id VARCHAR(255) NOT NULL,
    provider_team_id VARCHAR(255),
    season_key VARCHAR(255) NOT NULL,
    season_label VARCHAR(255) NOT NULL,
    participation_years VARCHAR(255),
    UNIQUE (run_item_id, provider_competition_id, season_key)
);

CREATE SEQUENCE player_performance_metric_observations_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_performance_metric_observations (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_performance_metric_observations_id_seq'),
    context_id BIGINT NOT NULL REFERENCES player_performance_contexts(id),
    metric_key VARCHAR(255) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    derivation VARCHAR(32) NOT NULL,
    value NUMERIC,
    availability VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    observed_precision INTEGER,
    observed_at TIMESTAMPTZ NOT NULL,
    UNIQUE (context_id, metric_key),
    CHECK ((availability = 'PRESENT' AND value IS NOT NULL)
        OR (availability <> 'PRESENT' AND value IS NULL)),
    CHECK (observed_precision IS NULL OR observed_precision >= 0)
);
CREATE INDEX ix_statistics_observations_current
    ON player_performance_metric_observations (metric_key, observed_at DESC, id DESC)
    WHERE availability = 'PRESENT';

CREATE SEQUENCE player_metric_comparisons_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_metric_comparisons (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_metric_comparisons_id_seq'),
    new_observation_id BIGINT NOT NULL REFERENCES player_performance_metric_observations(id),
    previous_other_source_observation_id BIGINT REFERENCES player_performance_metric_observations(id),
    tolerance NUMERIC,
    difference NUMERIC,
    result VARCHAR(32) NOT NULL,
    compared_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_statistics_comparisons_observation
    ON player_metric_comparisons (new_observation_id, compared_at DESC, id DESC);

CREATE SEQUENCE player_statistics_incidents_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_statistics_incidents (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_statistics_incidents_id_seq'),
    run_item_id BIGINT NOT NULL REFERENCES player_statistics_run_items(id),
    section VARCHAR(255),
    code VARCHAR(255) NOT NULL,
    provider_status INTEGER,
    attempt INTEGER NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CHECK (attempt IN (1, 2))
);
CREATE INDEX ix_statistics_incidents_item
    ON player_statistics_incidents (run_item_id, occurred_at, id);
