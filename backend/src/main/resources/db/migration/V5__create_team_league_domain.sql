-- Release 1: esquema aditivo. La retirada de textos requiere Release 2 y gate humano.
CREATE SEQUENCE leagues_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE leagues (
    id BIGINT PRIMARY KEY DEFAULT nextval('leagues_id_seq'),
    name VARCHAR(255) NOT NULL CHECK (btrim(name) <> '')
);
ALTER SEQUENCE leagues_id_seq OWNED BY leagues.id;

CREATE SEQUENCE teams_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE teams (
    id BIGINT PRIMARY KEY DEFAULT nextval('teams_id_seq'),
    name VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    league_id BIGINT NOT NULL REFERENCES leagues(id),
    current BOOLEAN NOT NULL
);
ALTER SEQUENCE teams_id_seq OWNED BY teams.id;
CREATE INDEX idx_teams_league_id ON teams(league_id);

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

ALTER TABLE players ADD COLUMN team_id BIGINT REFERENCES teams(id);
CREATE INDEX idx_players_team_id ON players(team_id);
-- Player conserva varias referencias legacy del mismo proveedor.
-- La UNIQUE(provider, external_id) de V3 ya impide compartir una identidad entre propietarios.

CREATE SEQUENCE pending_review_cases_id_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE pending_review_cases (
    id BIGINT PRIMARY KEY DEFAULT nextval('pending_review_cases_id_seq'),
    category VARCHAR(64) NOT NULL,
    cause_code VARCHAR(64) NOT NULL CHECK (btrim(cause_code) <> ''),
    subject_type VARCHAR(16) NOT NULL CHECK (subject_type IN ('PLAYER', 'TEAM', 'LEAGUE')),
    subject_id BIGINT,
    subject_provider VARCHAR(64),
    subject_external_id VARCHAR(255),
    case_key VARCHAR(2048) NOT NULL CHECK (btrim(case_key) <> ''),
    first_detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    evidence JSONB NOT NULL,
    CONSTRAINT uk_pending_review_cases_case_key UNIQUE (case_key),
    CONSTRAINT ck_pending_review_cases_dates CHECK (last_detected_at >= first_detected_at),
    CONSTRAINT ck_pending_review_cases_category CHECK (
        category IN ('LEGACY_TEAM_ASSOCIATION', 'PLAYER_TEAM_UNRESOLVED',
                     'TEAM_LEAGUE_UNRESOLVED', 'EXTERNAL_IDENTITY_CONFLICT',
                     'PLAYER_OPTIONAL_CONFLICT', 'INVALID_SUBJECT_DATA')),
    CONSTRAINT ck_pending_review_cases_external_subject CHECK (
        (subject_provider IS NULL AND subject_external_id IS NULL)
        OR (subject_provider IS NOT NULL AND subject_external_id IS NOT NULL
            AND btrim(subject_external_id) <> ''))
);
ALTER SEQUENCE pending_review_cases_id_seq OWNED BY pending_review_cases.id;
CREATE INDEX idx_pending_review_cases_subject ON pending_review_cases(subject_type, subject_id);

CREATE TABLE team_resolution_attempts (
    team_id BIGINT PRIMARY KEY REFERENCES teams(id),
    last_call_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_call_name VARCHAR(255) NOT NULL,
    technical_result VARCHAR(64),
    last_valid_evaluation_at TIMESTAMP WITH TIME ZONE,
    last_valid_evaluation_name VARCHAR(255),
    valid_result VARCHAR(64),
    retry_not_before TIMESTAMP WITH TIME ZONE
);

CREATE TABLE catalog_transition (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    completed_at TIMESTAMP WITH TIME ZONE
);
INSERT INTO catalog_transition (id, completed_at) VALUES (1, NULL);
