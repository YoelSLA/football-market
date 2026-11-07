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

ALTER SEQUENCE player_statistics_run_items_id_seq OWNED BY player_statistics_run_items.id;
CREATE INDEX ix_statistics_items_run_result
    ON player_statistics_run_items(run_id, result, id);
CREATE INDEX ix_statistics_items_campaign_player
    ON player_statistics_run_items(extraction_date, source, player_id);
CREATE UNIQUE INDEX uq_statistics_item_completed_campaign
    ON player_statistics_run_items(extraction_date, source, player_id)
    WHERE result = 'COMPLETED';
