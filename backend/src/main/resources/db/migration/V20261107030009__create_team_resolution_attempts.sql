CREATE TABLE team_resolution_attempts (
    team_id BIGINT PRIMARY KEY REFERENCES teams(id),
    last_call_at TIMESTAMPTZ NOT NULL,
    last_call_name VARCHAR(255) NOT NULL,
    technical_result VARCHAR(64),
    last_valid_evaluation_at TIMESTAMPTZ,
    last_valid_evaluation_name VARCHAR(255),
    valid_result VARCHAR(64),
    retry_not_before TIMESTAMPTZ
);
