package com.trading.nevcoin.trade.application;

import com.trading.nevcoin.market.domain.MarketTick;
import com.trading.nevcoin.trade.domain.PaperPosition;
import com.trading.nevcoin.trade.domain.TradeAction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MomentumDecisionEngineTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");
    private final PaperTradingProperties properties = new PaperTradingProperties();
    private final MomentumDecisionEngine engine = new MomentumDecisionEngine(properties);

    @Test
    void buysOnlyWhenThereIsNoPositionAndMomentumClearsEntryThreshold() {
        assertEquals(TradeAction.BUY, engine.evaluate(tick("8"), Optional.empty()).action());
    }

    @Test
    void sellsAnExistingPositionWhenMomentumCrossesExitThreshold() {
        PaperPosition position = new PaperPosition("mint", BigDecimal.ONE, BigDecimal.ONE, NOW);
        assertEquals(TradeAction.SELL, engine.evaluate(tick("-6"), Optional.of(position)).action());
    }

    @Test
    void holdsWhenTheActionConditionsAreNotMet() {
        PaperPosition position = new PaperPosition("mint", BigDecimal.ONE, BigDecimal.ONE, NOW);
        assertEquals(TradeAction.HOLD, engine.evaluate(tick("2"), Optional.of(position)).action());
    }

    private MarketTick tick(String momentum) {
        return new MarketTick("mint", NOW, NOW, BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.TEN, new BigDecimal(momentum), "test");
    }
}
