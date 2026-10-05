package com.trading.nevcoin.market.application.ports;

import com.trading.nevcoin.market.domain.MarketSignal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface MarketSignalQueryPort {

    List<SignalSummary> activeSignals(Instant now);

    record SignalSummary(
            String tokenAddress,
            MarketSignal.SignalType type,
            BigDecimal strength,
            Instant observedAt,
            Instant expiresAt,
            List<String> evidence,
            BigDecimal priceUsd,
            BigDecimal liquidityUsd,
            BigDecimal volume24hUsd,
            BigDecimal netFlow5m) {
    }
}
