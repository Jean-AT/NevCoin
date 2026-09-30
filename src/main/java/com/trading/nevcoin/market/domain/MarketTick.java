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
        String sourceQuality,
        BigDecimal buyVolume5m,
        BigDecimal sellVolume5m,
        BigDecimal netFlow5m,
        int uniqueBuyers5m,
        int uniqueSellers5m) {

    public MarketTick(
            String tokenAddress,
            Instant observedAt,
            Instant sourceTimestamp,
            BigDecimal priceUsd,
            BigDecimal liquidityUsd,
            BigDecimal volume24hUsd,
            BigDecimal priceChange24hPercent,
            String sourceQuality) {
        this(tokenAddress, observedAt, sourceTimestamp, priceUsd, liquidityUsd, volume24hUsd,
                priceChange24hPercent, sourceQuality, null, null, null, 0, 0);
    }
}
