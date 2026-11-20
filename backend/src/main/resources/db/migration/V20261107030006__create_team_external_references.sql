CREATE SEQUENCE team_external_references_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE team_external_references (
    id BIGINT PRIMARY KEY DEFAULT nextval('team_external_references_id_seq'),
    team_id BIGINT NOT NULL REFERENCES teams(id),
    provider VARCHAR(64) NOT NULL,
    external_id VARCHAR(255) NOT NULL CHECK (btrim(external_id) <> ''),
    CONSTRAINT uk_team_external_references_provider_external_id UNIQUE (provider, external_id),
    CONSTRAINT uk_team_external_references_owner_provider UNIQUE (team_id, provider)
);

ALTER SEQUENCE team_external_references_id_seq OWNED BY team_external_references.id;
