CREATE TABLE social_signals (
    id VARCHAR(36) PRIMARY KEY,
    token_address VARCHAR(100) NOT NULL,
    signal_type VARCHAR(64) NOT NULL,
    strength NUMERIC(20, 8) NOT NULL,
    confidence NUMERIC(20, 8) NOT NULL,
    first_observed_at TIMESTAMPTZ NOT NULL,
    last_observed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    source_count INTEGER NOT NULL,
    unique_source_count INTEGER NOT NULL,
    algorithm_version VARCHAR(64) NOT NULL
);
CREATE INDEX social_signals_token_active_idx ON social_signals (token_address, expires_at DESC);
CREATE TABLE social_signal_evidence (
    signal_id VARCHAR(36) NOT NULL REFERENCES social_signals(id) ON DELETE CASCADE,
    source_event_id VARCHAR(512) NOT NULL,
    source_account_id VARCHAR(512),
    evidence_type VARCHAR(64) NOT NULL,
    value TEXT,
    observed_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE cross_domain_signals (
    id VARCHAR(36) PRIMARY KEY,
    token_address VARCHAR(100) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    strength NUMERIC(20, 8) NOT NULL,
    confidence NUMERIC(20, 8) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    algorithm_version VARCHAR(64) NOT NULL
);
CREATE INDEX cross_domain_signals_token_active_idx ON cross_domain_signals (token_address, expires_at DESC);
CREATE INDEX cross_domain_signals_priority_active_idx ON cross_domain_signals (priority, expires_at DESC);
CREATE TABLE cross_domain_signal_components (
    signal_id VARCHAR(36) NOT NULL REFERENCES cross_domain_signals(id) ON DELETE CASCADE,
    component VARCHAR(32) NOT NULL
);
CREATE TABLE cross_domain_signal_evidence (
    signal_id VARCHAR(36) NOT NULL REFERENCES cross_domain_signals(id) ON DELETE CASCADE,
    component VARCHAR(32) NOT NULL,
    source_id VARCHAR(512) NOT NULL,
    description TEXT NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL
);
