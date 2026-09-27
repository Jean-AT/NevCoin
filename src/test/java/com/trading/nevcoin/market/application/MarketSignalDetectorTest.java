package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.domain.MarketSignal;
import com.trading.nevcoin.market.domain.MarketSnapshot;
import com.trading.nevcoin.market.domain.MarketTick;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketSignalDetectorTest {

    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");

    @Test
    void detectsMomentumWithoutProducingTradeAdvice() {
        MarketProperties properties = new MarketProperties();
        MarketTick tick = new MarketTick("mint", NOW, NOW, decimal("1.20"), decimal("1000"), decimal("300"),
                decimal("8"), "test");
        MarketSnapshot snapshot = new MarketSnapshot("mint", NOW, decimal("1.20"), decimal("1000"), decimal("300"),
                null, null, null, null, 0);

        List<MarketSignal> signals = new MarketSignalDetector().detect(tick, snapshot, null, properties);

        assertEquals(MarketSignal.SignalType.MOMENTUM_DETECTED, signals.getFirst().type());
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
