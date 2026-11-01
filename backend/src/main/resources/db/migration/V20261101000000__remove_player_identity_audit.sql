-- Solo se retira auditoría de sincronización; referencias y casos de revisión se conservan.
DROP TABLE player_identity_discarded_rows;
DROP TABLE player_identity_sync_items;
DROP TABLE whoscored_identity_candidates;
DROP TABLE player_identity_league_discoveries;
DROP TABLE player_identity_sync_runs;

DROP SEQUENCE player_identity_discarded_rows_id_seq;
DROP SEQUENCE player_identity_sync_items_id_seq;
DROP SEQUENCE whoscored_identity_candidates_id_seq;
DROP SEQUENCE player_identity_league_discoveries_id_seq;
DROP SEQUENCE player_identity_sync_runs_id_seq;
