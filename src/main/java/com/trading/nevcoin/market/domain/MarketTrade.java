package com.trading.nevcoin.market.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketTrade(
        String tokenAddress,
        Instant observedAt,
        Instant sourceTimestamp,
        String transactionSignature,
        String traderAddress,
        Side side,
        BigDecimal amountUsd,
        BigDecimal priceUsd,
        String sourceQuality) {

    public enum Side {
        BUY,
        SELL
    }
}
