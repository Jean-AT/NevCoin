CREATE TABLE social_raw_events (
    id VARCHAR(36) PRIMARY KEY,
    provider VARCHAR(64) NOT NULL,
    source_id VARCHAR(256) NOT NULL,
    source_account_id VARCHAR(256),
    external_event_id VARCHAR(512) NOT NULL,
    canonical_url VARCHAR(1024),
    published_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ NOT NULL,
    raw_text TEXT NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    CONSTRAINT social_raw_events_identity_unique UNIQUE (provider, source_id, external_event_id)
);

CREATE INDEX social_raw_events_received_at_idx ON social_raw_events (received_at DESC);

CREATE TABLE social_provider_cursors (
    source_id VARCHAR(256) PRIMARY KEY,
    cursor VARCHAR(512),
    fetched_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE social_processing_failures (
    id BIGSERIAL PRIMARY KEY,
    source_id VARCHAR(256) NOT NULL,
    stage VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    message TEXT,
    retryable BOOLEAN NOT NULL,
    failed_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL
);

CREATE INDEX social_processing_failures_source_failed_at_idx
    ON social_processing_failures (source_id, failed_at DESC);
