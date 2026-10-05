CREATE TABLE market_alert_preferences (
    token_address VARCHAR(100) PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
