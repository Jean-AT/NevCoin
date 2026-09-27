package com.trading.nevcoin.market.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketTick(
        String tokenAddress,
        Instant observedAt,
        Instant sourceTimestamp,
        BigDecimal priceUsd,
        BigDecimal liquidityUsd,
        BigDecimal volume24hUsd,
        BigDecimal priceChange24hPercent,
        String sourceQuality) {
}
