package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.domain.MarketTrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradeWindowAggregatorTest {

    private static final Instant NOW = Instant.parse("2026-09-30T00:05:00Z");

    @Test
    void aggregatesPressureAndUniqueTradersOverFiveMinutes() {
        TradeWindowAggregator aggregator = new TradeWindowAggregator();

        aggregator.add(trade("buyer-a", MarketTrade.Side.BUY, "10", NOW.minusSeconds(60)));
        aggregator.add(trade("buyer-a", MarketTrade.Side.BUY, "5", NOW.minusSeconds(30)));
        TradeWindowAggregator.TradeMetrics metrics = aggregator.add(
                trade("seller-a", MarketTrade.Side.SELL, "4", NOW));

        assertEquals(new BigDecimal("15"), metrics.buyVolume5m());
        assertEquals(new BigDecimal("4"), metrics.sellVolume5m());
        assertEquals(new BigDecimal("11"), metrics.netFlow5m());
        assertEquals(1, metrics.uniqueBuyers5m());
        assertEquals(1, metrics.uniqueSellers5m());
    }

    @Test
    void expiresTradesOutsideTheFiveMinuteWindow() {
        TradeWindowAggregator aggregator = new TradeWindowAggregator();

        aggregator.add(trade("buyer-a", MarketTrade.Side.BUY, "10", NOW.minusSeconds(301)));
        TradeWindowAggregator.TradeMetrics metrics = aggregator.add(
                trade("seller-a", MarketTrade.Side.SELL, "4", NOW));

        assertEquals(BigDecimal.ZERO, metrics.buyVolume5m());
        assertEquals(new BigDecimal("4"), metrics.sellVolume5m());
        assertEquals(0, metrics.uniqueBuyers5m());
        assertEquals(1, metrics.uniqueSellers5m());
    }

    private MarketTrade trade(String trader, MarketTrade.Side side, String amount, Instant timestamp) {
        return new MarketTrade("mint", timestamp, timestamp, null, trader, side,
                new BigDecimal(amount), new BigDecimal("1"), "test");
    }
}
