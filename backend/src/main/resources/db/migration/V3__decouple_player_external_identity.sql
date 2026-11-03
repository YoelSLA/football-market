ALTER TABLE players RENAME COLUMN id TO legacy_external_id;
ALTER TABLE players DROP CONSTRAINT players_pkey;

CREATE SEQUENCE players_id_seq START WITH 1 INCREMENT BY 1;
ALTER TABLE players ADD COLUMN id BIGINT NOT NULL DEFAULT nextval('players_id_seq');
ALTER SEQUENCE players_id_seq OWNED BY players.id;
ALTER TABLE players ADD PRIMARY KEY (id);
ALTER TABLE players ADD COLUMN date_of_birth DATE;
ALTER TABLE players ADD COLUMN nationality VARCHAR(255);
ALTER TABLE players ADD COLUMN image_url VARCHAR(2048);

CREATE SEQUENCE player_external_references_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE player_external_references (
    id BIGINT PRIMARY KEY DEFAULT nextval('player_external_references_id_seq'),
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NOT NULL CHECK (btrim(external_id) <> ''),
    CONSTRAINT uk_player_external_references_provider_external_id
        UNIQUE (provider, external_id) NOT DEFERRABLE
);
ALTER SEQUENCE player_external_references_id_seq OWNED BY player_external_references.id;
CREATE INDEX idx_player_external_references_player_id ON player_external_references(player_id);

INSERT INTO player_external_references (player_id, provider, external_id)
SELECT id, 'FOOTBALL_DATA', legacy_external_id::text FROM players;

ALTER TABLE players DROP COLUMN legacy_external_id;
SELECT setval('players_id_seq', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM players;
SELECT setval('player_external_references_id_seq', COALESCE(MAX(id), 1), MAX(id) IS NOT NULL)
FROM player_external_references;
