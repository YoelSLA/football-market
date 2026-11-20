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

ALTER SEQUENCE player_statistics_incidents_id_seq OWNED BY player_statistics_incidents.id;
CREATE INDEX ix_statistics_incidents_item
    ON player_statistics_incidents(run_item_id, occurred_at, id);
