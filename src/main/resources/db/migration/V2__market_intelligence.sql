CREATE TABLE market_ticks (
    id VARCHAR(36) PRIMARY KEY,
    token_address VARCHAR(100) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    source_timestamp TIMESTAMPTZ NOT NULL,
    price_usd NUMERIC(30, 12),
    liquidity_usd NUMERIC(30, 12),
    volume24h_usd NUMERIC(30, 12),
    price_change24h_percent NUMERIC(30, 12),
    source_quality VARCHAR(50) NOT NULL
);

CREATE INDEX idx_market_ticks_token_observed
    ON market_ticks (token_address, observed_at DESC);

CREATE TABLE market_snapshots (
    id VARCHAR(36) PRIMARY KEY,
    token_address VARCHAR(100) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    price_usd NUMERIC(30, 12),
    liquidity_usd NUMERIC(30, 12),
    volume24h_usd NUMERIC(30, 12),
    price_change1m NUMERIC(30, 12),
    price_change5m NUMERIC(30, 12),
    price_change15m NUMERIC(30, 12),
    volatility5m NUMERIC(30, 12),
    data_freshness_ms BIGINT NOT NULL
);

CREATE INDEX idx_market_snapshots_token_timestamp
    ON market_snapshots (token_address, timestamp DESC);

CREATE TABLE market_signals (
    id VARCHAR(36) PRIMARY KEY,
    token_address VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    strength NUMERIC(30, 12) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    source_quality VARCHAR(50) NOT NULL
);

CREATE TABLE market_signal_evidence (
    signal_id VARCHAR(36) NOT NULL REFERENCES market_signals(id),
    evidence TEXT NOT NULL
);
