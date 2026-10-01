package com.trading.nevcoin.notification.application.ports;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface TokenMarketDataPort {

    Optional<TokenMarketData> find(String mintAddress);

    record TokenMarketData(
            String mintAddress,
            String symbol,
            String name,
            BigDecimal priceUsd,
            BigDecimal marketCapUsd,
            BigDecimal liquidityUsd,
            BigDecimal volume24hUsd,
            BigDecimal priceChange24hPercent,
            int buys5m,
            int sells5m,
            Instant observedAt,
            String source) {
    }
}
