package com.trading.nevcoin.signal.domain;

import java.time.Instant;

public record SignalEvidence(SignalComponent component, String sourceId, String description, Instant observedAt) {
}
