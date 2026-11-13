CREATE TABLE user_credits (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    available_credits BIGINT NOT NULL DEFAULT 0 CHECK (available_credits >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
