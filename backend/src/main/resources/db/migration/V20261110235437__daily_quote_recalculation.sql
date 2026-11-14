-- Cotización diaria por jugador y auditoría de múltiples corridas.
-- Los datos incompatibles hacen fallar la migración; no se transforman ni eliminan.
ALTER TABLE quote_executions DROP CONSTRAINT uq_quote_executions_date;
ALTER TABLE quote_executions ALTER COLUMN finished_at DROP NOT NULL;
ALTER TABLE quote_executions ADD CONSTRAINT ck_quote_execution_finished_at
    CHECK (finished_at IS NULL OR finished_at >= started_at);
ALTER TABLE quote_executions ADD CONSTRAINT ck_quote_execution_counter_partition
    CHECK (finished_at IS NULL OR priced_players =
        recomputed_rows + carried_forward_rows + not_repriced_players + failed_players);

DROP INDEX ix_quote_executions_date;
CREATE INDEX ix_quote_executions_date
    ON quote_executions(quote_date DESC, started_at DESC, id DESC);

ALTER TABLE player_quotes DROP CONSTRAINT uq_player_quote;
ALTER TABLE player_quotes ADD CONSTRAINT uq_player_quote UNIQUE (player_id, effective_date);
ALTER TABLE player_quotes ADD COLUMN execution_id BIGINT;
ALTER TABLE player_quotes ADD CONSTRAINT fk_player_quote_execution
    FOREIGN KEY (execution_id) REFERENCES quote_executions(id);
ALTER TABLE player_quotes ADD CONSTRAINT ck_player_quote_execution
    CHECK ((origin = 'INITIAL' AND execution_id IS NULL)
        OR (origin <> 'INITIAL' AND execution_id IS NOT NULL));
CREATE INDEX ix_player_quotes_execution ON player_quotes(execution_id);
