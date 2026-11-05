-- Registra el origen de cada corrida y exige que las nuevas inserciones lo declaren.
ALTER TABLE quote_executions
    ADD COLUMN origin VARCHAR(16) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE quote_executions ALTER COLUMN origin DROP DEFAULT;

ALTER TABLE quote_executions
    ADD CONSTRAINT ck_quote_execution_origin CHECK (origin IN ('MANUAL', 'AUTOMATIC'));
