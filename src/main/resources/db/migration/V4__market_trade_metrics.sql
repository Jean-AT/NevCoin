ALTER TABLE market_ticks
    ADD COLUMN buy_volume5m NUMERIC(30, 12),
    ADD COLUMN sell_volume5m NUMERIC(30, 12),
    ADD COLUMN net_flow5m NUMERIC(30, 12),
    ADD COLUMN unique_buyers5m INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN unique_sellers5m INTEGER NOT NULL DEFAULT 0;

ALTER TABLE market_snapshots
    ADD COLUMN buy_volume5m NUMERIC(30, 12),
    ADD COLUMN sell_volume5m NUMERIC(30, 12),
    ADD COLUMN net_flow5m NUMERIC(30, 12),
    ADD COLUMN unique_buyers5m INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN unique_sellers5m INTEGER NOT NULL DEFAULT 0;
