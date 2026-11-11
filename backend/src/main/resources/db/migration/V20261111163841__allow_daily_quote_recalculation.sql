-- Detener ante colisiones heredadas; su resolución requiere una decisión humana.
-- Diagnóstico: SELECT player_id, effective_date, count(*) FROM player_quotes
-- GROUP BY player_id, effective_date HAVING count(*) > 1;
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM player_quotes
        GROUP BY player_id, effective_date HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Existen cotizaciones duplicadas por jugador y fecha; resolver con autorización humana antes de migrar';
    END IF;
END;
$$;

ALTER TABLE quote_executions DROP CONSTRAINT uq_quote_executions_date;
CREATE UNIQUE INDEX uq_quote_executions_automatic_date
    ON quote_executions (quote_date) WHERE origin = 'AUTOMATIC';

ALTER TABLE player_quotes DROP CONSTRAINT uq_player_quote;
ALTER TABLE player_quotes ADD CONSTRAINT uq_player_quote UNIQUE (player_id, effective_date);
