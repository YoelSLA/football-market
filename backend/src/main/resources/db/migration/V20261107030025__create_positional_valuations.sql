CREATE SEQUENCE player_positional_valuations_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE player_positional_valuations (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_positional_valuations_id_seq'),
    player_id BIGINT NOT NULL REFERENCES players(id),
    valuation_date DATE NOT NULL,
    strategy_name VARCHAR(64) NOT NULL,
    strategy_version VARCHAR(16) NOT NULL,
    general_position VARCHAR(32) NOT NULL,
    source_summary VARCHAR(16) NOT NULL,
    source_extraction_date DATE NOT NULL,
    positive_score NUMERIC(18,6) NOT NULL,
    performance_penalty NUMERIC(18,6),
    performance_adjustment NUMERIC(18,6),
    discipline_penalty NUMERIC(18,6) NOT NULL,
    discipline_adjustment NUMERIC(18,6) NOT NULL,
    raw_score NUMERIC(18,6) NOT NULL,
    confidence NUMERIC(18,6) NOT NULL,
    final_score NUMERIC(18,6) NOT NULL,
    imputed_metrics TEXT[] NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_player_positional_valuation
        UNIQUE (player_id, valuation_date, strategy_name, strategy_version),
    CONSTRAINT ck_positional_strategy
        CHECK (strategy_name = 'POSITIONAL_PERFORMANCE' AND strategy_version = 'v1'),
    CONSTRAINT ck_positional_position
        CHECK (general_position IN ('GOALKEEPER','DEFENDER','MIDFIELDER','FORWARD')),
    CONSTRAINT ck_positional_source CHECK (source_summary IN ('JSON','SCRAPING','MIXED')),
    CONSTRAINT ck_positional_ranges
        CHECK (positive_score BETWEEN 0 AND 1 AND discipline_penalty BETWEEN 0 AND 1
            AND confidence BETWEEN 0 AND 1 AND final_score BETWEEN 0 AND 1),
    CONSTRAINT ck_positional_goalkeeper CHECK (
        (general_position = 'GOALKEEPER' AND performance_penalty IS NULL
            AND performance_adjustment IS NULL)
        OR (general_position <> 'GOALKEEPER' AND performance_penalty IS NOT NULL
            AND performance_adjustment IS NOT NULL))
);

ALTER SEQUENCE player_positional_valuations_id_seq
    OWNED BY player_positional_valuations.id;
CREATE INDEX ix_positional_valuation_date
    ON player_positional_valuations(valuation_date DESC);
CREATE INDEX ix_positional_final_score ON player_positional_valuations(final_score DESC);
CREATE INDEX ix_positional_player_date
    ON player_positional_valuations(player_id, valuation_date DESC);
