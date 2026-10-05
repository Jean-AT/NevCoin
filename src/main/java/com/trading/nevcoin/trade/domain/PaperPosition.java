package com.trading.nevcoin.trade.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PaperPosition(
        String tokenAddress,
        BigDecimal quantity,
        BigDecimal averageEntryPriceUsd,
        Instant updatedAt) {
}
