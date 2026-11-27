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
    first_detected_at TIMESTAMPTZ NOT NULL,
    last_detected_at TIMESTAMPTZ NOT NULL,
    evidence JSONB NOT NULL,
    CONSTRAINT uk_pending_review_cases_case_key UNIQUE (case_key),
    CONSTRAINT ck_pending_review_cases_dates CHECK (last_detected_at >= first_detected_at),
    CONSTRAINT ck_pending_review_cases_category CHECK (
        category IN ('PLAYER_TEAM_UNRESOLVED', 'TEAM_LEAGUE_UNRESOLVED',
                     'EXTERNAL_IDENTITY_CONFLICT', 'PLAYER_OPTIONAL_CONFLICT',
                     'INVALID_SUBJECT_DATA')),
    CONSTRAINT ck_pending_review_cases_external_subject CHECK (
        (subject_provider IS NULL AND subject_external_id IS NULL)
        OR (subject_provider IS NOT NULL AND subject_external_id IS NOT NULL
            AND btrim(subject_external_id) <> ''))
);

ALTER SEQUENCE pending_review_cases_id_seq OWNED BY pending_review_cases.id;
CREATE INDEX idx_pending_review_cases_subject ON pending_review_cases(subject_type, subject_id);
