package com.trading.nevcoin.paper.validation.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record ReplayPricePoint(String tokenAddress, Instant observedAt, BigDecimal priceUsd, String sourceId) {
    public ReplayPricePoint {
        if (tokenAddress == null || tokenAddress.isBlank() || observedAt == null || priceUsd == null || priceUsd.signum() <= 0) {
            throw new IllegalArgumentException("A valid token, timestamp, and positive price are required");
        }
        sourceId = sourceId == null ? "replay" : sourceId;
    }
}
