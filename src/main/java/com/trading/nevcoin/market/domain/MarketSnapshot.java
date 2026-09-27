package com.trading.nevcoin.market.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketSnapshot(
        String tokenAddress,
        Instant timestamp,
        BigDecimal priceUsd,
        BigDecimal liquidityUsd,
        BigDecimal volume24hUsd,
        BigDecimal priceChange1m,
        BigDecimal priceChange5m,
        BigDecimal priceChange15m,
        BigDecimal volatility5m,
        long dataFreshnessMs) {
}
