CREATE TABLE paper_settings (
    id VARCHAR(50) PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    cash_usd NUMERIC(30, 12) NOT NULL,
    initial_cash_usd NUMERIC(30, 12) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE paper_positions (
    token_address VARCHAR(100) PRIMARY KEY,
    quantity NUMERIC(30, 18) NOT NULL,
    average_entry_price_usd NUMERIC(30, 12) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE paper_trades (
    id VARCHAR(36) PRIMARY KEY,
    token_address VARCHAR(100) NOT NULL,
    action VARCHAR(10) NOT NULL,
    quantity NUMERIC(30, 18) NOT NULL,
    price_usd NUMERIC(30, 12) NOT NULL,
    notional_usd NUMERIC(30, 12) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    executed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_paper_trades_executed_at ON paper_trades (executed_at DESC);
