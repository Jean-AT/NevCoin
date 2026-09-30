CREATE TABLE tokens (
    mint_address VARCHAR(100) PRIMARY KEY,
    symbol VARCHAR(32),
    name VARCHAR(128),
    decimals INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ,
    first_seen_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE INDEX idx_tokens_symbol ON tokens (lower(symbol));
