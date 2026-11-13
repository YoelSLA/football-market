CREATE SEQUENCE league_external_references_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE league_external_references (
    id BIGINT PRIMARY KEY DEFAULT nextval('league_external_references_id_seq'),
    league_id BIGINT NOT NULL REFERENCES leagues(id),
    provider VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NOT NULL CHECK (btrim(external_id) <> ''),
    CONSTRAINT uk_league_external_references_provider_external_id UNIQUE (provider, external_id),
    CONSTRAINT uk_league_external_references_owner_provider UNIQUE (league_id, provider)
);

ALTER SEQUENCE league_external_references_id_seq OWNED BY league_external_references.id;
