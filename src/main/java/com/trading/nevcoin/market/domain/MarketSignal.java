package com.trading.nevcoin.market.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MarketSignal(
        String tokenAddress,
        SignalType type,
        BigDecimal strength,
        Instant observedAt,
        Instant expiresAt,
        List<String> evidence,
        String sourceQuality) {

    public enum SignalType {
        MOMENTUM_DETECTED,
        VOLUME_SPIKE_DETECTED,
        LIQUIDITY_DROP_DETECTED
    }
}
