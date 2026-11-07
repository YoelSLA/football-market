CREATE SEQUENCE player_quotes_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE player_quotes (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_quotes_id_seq'),
    player_id BIGINT NOT NULL REFERENCES players(id),
    effective_date DATE NOT NULL,
    price BIGINT NOT NULL,
    previous_price BIGINT,
    multiplier NUMERIC(18,6) NOT NULL,
    score NUMERIC(18,6),
    strategy_name VARCHAR(64),
    strategy_version VARCHAR(16),
    source_valuation_date DATE,
    origin VARCHAR(24) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_player_quote UNIQUE (player_id, effective_date, origin),
    CONSTRAINT ck_player_quote_price CHECK (price > 0),
    CONSTRAINT ck_player_quote_previous_price CHECK (
        (origin = 'INITIAL' AND previous_price IS NULL AND multiplier = 1 AND score IS NULL)
        OR (origin <> 'INITIAL' AND previous_price IS NOT NULL AND previous_price > 0)),
    CONSTRAINT ck_player_quote_origin
        CHECK (origin IN ('INITIAL', 'RECOMPUTED', 'CARRIED_FORWARD')),
    CONSTRAINT ck_player_quote_initial CHECK (
        (origin = 'INITIAL' AND strategy_name IS NULL AND strategy_version IS NULL
            AND source_valuation_date IS NULL) OR origin <> 'INITIAL'),
    CONSTRAINT ck_player_quote_carried CHECK (
        origin <> 'CARRIED_FORWARD'
        OR (score IS NULL AND source_valuation_date IS NULL AND multiplier = 1)),
    CONSTRAINT ck_player_quote_recomputed CHECK (
        origin <> 'RECOMPUTED'
        OR (score IS NOT NULL AND strategy_name IS NOT NULL AND strategy_version IS NOT NULL
            AND source_valuation_date IS NOT NULL))
);

ALTER SEQUENCE player_quotes_id_seq OWNED BY player_quotes.id;
CREATE INDEX ix_player_quotes_player_date
    ON player_quotes(player_id, effective_date DESC, calculated_at DESC);
CREATE INDEX ix_player_quotes_effective_price
    ON player_quotes(effective_date DESC, price DESC);
