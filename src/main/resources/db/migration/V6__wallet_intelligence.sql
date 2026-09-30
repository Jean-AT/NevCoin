CREATE TABLE tracked_wallets (
    address VARCHAR(100) PRIMARY KEY,
    alias VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL
);
