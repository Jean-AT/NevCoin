CREATE TABLE IF NOT EXISTS market_signal_evidence (
    signal_id VARCHAR(36) NOT NULL REFERENCES market_signals(id),
    evidence TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_market_signal_evidence_signal
    ON market_signal_evidence (signal_id);
