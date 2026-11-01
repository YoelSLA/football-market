-- PostgreSQL retira también los checks de la misma tabla que dependen de la columna.
ALTER TABLE player_statistics_extraction_runs DROP COLUMN max_players;
