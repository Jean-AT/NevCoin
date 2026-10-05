package com.trading.nevcoin.trade.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PaperTrade(
        String tokenAddress,
        TradeAction action,
        BigDecimal quantity,
        BigDecimal priceUsd,
        BigDecimal notionalUsd,
        String reason,
        Instant executedAt,
        BigDecimal realizedPnlUsd) {

    public PaperTrade(
            String tokenAddress,
            TradeAction action,
            BigDecimal quantity,
            BigDecimal priceUsd,
            BigDecimal notionalUsd,
            String reason,
            Instant executedAt) {
        this(tokenAddress, action, quantity, priceUsd, notionalUsd, reason, executedAt, null);
    }

    public PaperTrade withRealizedPnlUsd(BigDecimal value) {
        return new PaperTrade(tokenAddress, action, quantity, priceUsd, notionalUsd, reason, executedAt, value);
    }
}
