package com.trading.nevcoin.trade.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TradeDecision(
        String tokenAddress,
        TradeAction action,
        BigDecimal confidence,
        Instant observedAt,
        List<String> evidence) {
}
