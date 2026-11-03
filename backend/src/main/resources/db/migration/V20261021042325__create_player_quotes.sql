-- create_player_quotes
CREATE SEQUENCE player_quotes_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE quote_executions_id_seq START WITH 1 INCREMENT BY 1;

-- Historial de cotizaciones: la fila de vigencia máxima es la cotización vigente.
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
    CONSTRAINT ck_player_quote_previous_price CHECK ((origin = 'INITIAL' AND previous_price IS NULL AND multiplier = 1 AND score IS NULL) OR (origin <> 'INITIAL' AND previous_price IS NOT NULL AND previous_price > 0)),
    CONSTRAINT ck_player_quote_origin CHECK (origin IN ('INITIAL', 'RECOMPUTED', 'CARRIED_FORWARD')),
    CONSTRAINT ck_player_quote_initial CHECK ((origin = 'INITIAL' AND strategy_name IS NULL AND strategy_version IS NULL AND source_valuation_date IS NULL) OR origin <> 'INITIAL'),
    CONSTRAINT ck_player_quote_carried CHECK (origin <> 'CARRIED_FORWARD' OR (score IS NULL AND source_valuation_date IS NULL AND multiplier = 1)),
    CONSTRAINT ck_player_quote_recomputed CHECK (origin <> 'RECOMPUTED' OR (score IS NOT NULL AND strategy_name IS NOT NULL AND strategy_version IS NOT NULL AND source_valuation_date IS NOT NULL))
);
CREATE INDEX ix_player_quotes_player_date ON player_quotes (player_id, effective_date DESC, calculated_at DESC);
CREATE INDEX ix_player_quotes_effective_price ON player_quotes (effective_date DESC, price DESC);

-- Resumen append-only de cada corrida de cotización: una sola ejecución por fecha.
CREATE TABLE quote_executions (
    id BIGINT PRIMARY KEY DEFAULT nextval('quote_executions_id_seq'),
    quote_date DATE NOT NULL,
    strategy_name VARCHAR(64) NOT NULL,
    strategy_version VARCHAR(16) NOT NULL,
    adjustment_factor NUMERIC(18,6) NOT NULL,
    variation_cap NUMERIC(18,6) NOT NULL,
    min_price_floor BIGINT NOT NULL,
    priced_players INTEGER NOT NULL,
    recomputed_rows INTEGER NOT NULL,
    carried_forward_rows INTEGER NOT NULL,
    not_repriced_players INTEGER NOT NULL,
    failed_players INTEGER NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_quote_executions_date UNIQUE (quote_date),
    CONSTRAINT ck_quote_execution_factor CHECK (adjustment_factor BETWEEN 0.10 AND 1.00),
    CONSTRAINT ck_quote_execution_cap CHECK (variation_cap > 0 AND variation_cap <= 1),
    CONSTRAINT ck_quote_execution_floor CHECK (min_price_floor > 0),
    CONSTRAINT ck_quote_execution_counters CHECK (priced_players >= 0 AND recomputed_rows >= 0 AND carried_forward_rows >= 0 AND not_repriced_players >= 0 AND failed_players >= 0)
);
CREATE INDEX ix_quote_executions_date ON quote_executions (quote_date DESC, started_at DESC);
