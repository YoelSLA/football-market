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
CREATE UNIQUE INDEX uk_player_external_references_whoscored_player
    ON player_external_references(player_id) WHERE provider = 'WHOSCORED';
