ALTER TABLE social_events
    ADD COLUMN provider VARCHAR(64),
    ADD COLUMN source_id VARCHAR(256),
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD COLUMN processed_at TIMESTAMPTZ,
    ADD COLUMN alerted_at TIMESTAMPTZ,
    ADD COLUMN event_type VARCHAR(64),
    ADD COLUMN normalized_content TEXT,
    ADD COLUMN fingerprint VARCHAR(128),
    ADD COLUMN status VARCHAR(32),
    ADD COLUMN algorithm_version VARCHAR(64);

UPDATE social_events
SET provider = COALESCE(provider, 'legacy'),
    source_id = COALESCE(source_id, source),
    published_at = COALESCE(published_at, observed_at),
    processed_at = COALESCE(processed_at, received_at),
    event_type = COALESCE(event_type, 'UNKNOWN'),
    normalized_content = COALESCE(normalized_content, text),
    status = COALESCE(status, 'PUBLISHED'),
    algorithm_version = COALESCE(algorithm_version, 'legacy-v1');

ALTER TABLE social_events
    ALTER COLUMN provider SET NOT NULL,
    ALTER COLUMN source_id SET NOT NULL,
    ALTER COLUMN published_at SET NOT NULL,
    ALTER COLUMN processed_at SET NOT NULL,
    ALTER COLUMN event_type SET NOT NULL,
    ALTER COLUMN normalized_content SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN algorithm_version SET NOT NULL;

CREATE UNIQUE INDEX social_events_provider_source_external_idx
    ON social_events (provider, source_id, external_id);
CREATE INDEX social_events_fingerprint_idx ON social_events (fingerprint);
