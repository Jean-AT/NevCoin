CREATE TABLE social_source_states (
    source_id VARCHAR(256) PRIMARY KEY,
    last_attempt_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1024),
    last_latency_ms BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX social_source_states_next_attempt_idx
    ON social_source_states (next_attempt_at);
