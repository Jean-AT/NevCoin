CREATE TABLE notification_outbox (
    id VARCHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(256) NOT NULL UNIQUE,
    event_type VARCHAR(128),
    channel VARCHAR(64) NOT NULL,
    recipient VARCHAR(256) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ,
    claimed_until TIMESTAMPTZ,
    claim_token VARCHAR(128),
    CONSTRAINT notification_outbox_status_chk CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED'))
);

CREATE INDEX notification_outbox_claim_idx
    ON notification_outbox (status, next_attempt_at, claimed_until);
