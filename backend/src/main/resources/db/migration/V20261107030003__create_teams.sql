CREATE SEQUENCE teams_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE teams (
    id BIGINT PRIMARY KEY DEFAULT nextval('teams_id_seq'),
    name VARCHAR(255) NOT NULL CHECK (btrim(name) <> ''),
    league_id BIGINT NOT NULL REFERENCES leagues(id),
    current BOOLEAN NOT NULL
);

ALTER SEQUENCE teams_id_seq OWNED BY teams.id;
CREATE INDEX idx_teams_league_id ON teams(league_id);
