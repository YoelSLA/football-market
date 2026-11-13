CREATE SEQUENCE players_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE players (
    id BIGINT PRIMARY KEY DEFAULT nextval('players_id_seq'),
    name VARCHAR(255) NOT NULL,
    position VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    date_of_birth DATE,
    nationality VARCHAR(255),
    image_url VARCHAR(2048),
    fallback_image_url VARCHAR(2048),
    team_id BIGINT NOT NULL REFERENCES teams(id)
);

ALTER SEQUENCE players_id_seq OWNED BY players.id;
CREATE INDEX idx_players_team_id ON players(team_id);
