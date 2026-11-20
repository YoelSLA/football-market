CREATE SEQUENCE quote_executions_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE quote_executions (
    id BIGINT PRIMARY KEY DEFAULT nextval('quote_executions_id_seq'),
    quote_date DATE NOT NULL,
    strategy_name VARCHAR(64) NOT NULL,
    strategy_version VARCHAR(16) NOT NULL,
    origin VARCHAR(16) NOT NULL,
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
    CONSTRAINT ck_quote_execution_origin CHECK (origin IN ('MANUAL', 'AUTOMATIC')),
    CONSTRAINT ck_quote_execution_factor CHECK (adjustment_factor BETWEEN 0.10 AND 1.00),
    CONSTRAINT ck_quote_execution_cap CHECK (variation_cap > 0 AND variation_cap <= 1),
    CONSTRAINT ck_quote_execution_floor CHECK (min_price_floor > 0),
    CONSTRAINT ck_quote_execution_counters CHECK (
        priced_players >= 0 AND recomputed_rows >= 0 AND carried_forward_rows >= 0
        AND not_repriced_players >= 0 AND failed_players >= 0)
);

ALTER SEQUENCE quote_executions_id_seq OWNED BY quote_executions.id;
CREATE INDEX ix_quote_executions_date ON quote_executions(quote_date DESC, started_at DESC);
