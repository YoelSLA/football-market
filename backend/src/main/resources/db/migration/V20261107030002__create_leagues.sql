CREATE SEQUENCE leagues_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE leagues (
    id BIGINT PRIMARY KEY DEFAULT nextval('leagues_id_seq'),
    name VARCHAR(255) NOT NULL CHECK (btrim(name) <> '')
);

ALTER SEQUENCE leagues_id_seq OWNED BY leagues.id;
