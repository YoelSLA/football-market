-- Diagnóstico manual de run=6; PostgreSQL. No contiene escrituras ni IDs externos.
-- Ejecutar conectado a la base local de la aplicación, no a una base de testing.
BEGIN READ ONLY;
SET LOCAL statement_timeout = '15s';

-- Un resultado por jugador; evita multiplicar fallos por incidencias.
SELECT result, incident_code, count(*) AS players,
       round(avg(extract(epoch FROM (finished_at - started_at)))::numeric, 2) AS avg_seconds
FROM player_statistics_run_items
WHERE run_id = 6
GROUP BY result, incident_code
ORDER BY players DESC;

-- Las secciones son las registradas: un timeout puede haber perdido la categoría.
SELECT e.code, e.section, e.provider_status,
       count(DISTINCT i.player_id) AS players
FROM player_statistics_incidents e
JOIN player_statistics_run_items i ON i.id = e.run_item_id
WHERE i.run_id = 6
GROUP BY e.code, e.section, e.provider_status
ORDER BY players DESC;

-- Confirmados de la misma campaña antes de comenzar run=6 (hipótesis: 51).
SELECT count(DISTINCT i.player_id) AS completed_before_run
FROM player_statistics_run_items i
JOIN player_statistics_extraction_runs r ON r.id = 6
WHERE i.extraction_date = r.extraction_date AND i.source = r.source
  AND i.result = 'COMPLETED' AND i.finished_at < r.started_at;

-- Casos representativos; solo identificadores internos.
SELECT player_id, result, incident_code, started_at, finished_at
FROM player_statistics_run_items
WHERE run_id = 6 AND player_id IN (2483, 2484, 2491, 2487, 2539, 2567)
ORDER BY player_id;

COMMIT;
