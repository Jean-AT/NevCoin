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
        long dataFreshnessMs,
        BigDecimal buyVolume5m,
        BigDecimal sellVolume5m,
        BigDecimal netFlow5m,
        int uniqueBuyers5m,
        int uniqueSellers5m) {

    public MarketSnapshot(
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
        this(tokenAddress, timestamp, priceUsd, liquidityUsd, volume24hUsd, priceChange1m,
                priceChange5m, priceChange15m, volatility5m, dataFreshnessMs,
                null, null, null, 0, 0);
    }
}
