CREATE TABLE social_events (
    id VARCHAR(36) PRIMARY KEY,
    source VARCHAR(64) NOT NULL,
    external_id VARCHAR(512) NOT NULL,
    token_address VARCHAR(100),
    author VARCHAR(256),
    text TEXT NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    evidence TEXT,
    confidence NUMERIC(8, 6),
    CONSTRAINT social_events_source_external_id_unique UNIQUE (source, external_id)
);

CREATE INDEX social_events_observed_at_idx ON social_events (observed_at DESC);
CREATE INDEX social_events_token_observed_at_idx ON social_events (token_address, observed_at DESC);
