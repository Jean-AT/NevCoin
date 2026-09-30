package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.domain.MarketTrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class TradeWindowAggregator {

    private static final Duration WINDOW = Duration.ofMinutes(5);
    private final Map<String, Deque<MarketTrade>> tradesByToken = new HashMap<>();

    public synchronized TradeMetrics add(MarketTrade trade) {
        Deque<MarketTrade> trades = tradesByToken.computeIfAbsent(trade.tokenAddress(), ignored -> new ArrayDeque<>());
        trades.addLast(trade);
        removeExpired(trades, trade.observedAt());

        BigDecimal buyVolume = BigDecimal.ZERO;
        BigDecimal sellVolume = BigDecimal.ZERO;
        Set<String> buyers = new HashSet<>();
        Set<String> sellers = new HashSet<>();
        for (MarketTrade current : trades) {
            BigDecimal amount = current.amountUsd() == null ? BigDecimal.ZERO : current.amountUsd();
            if (current.side() == MarketTrade.Side.BUY) {
                buyVolume = buyVolume.add(amount);
                addIfPresent(buyers, current.traderAddress());
            } else {
                sellVolume = sellVolume.add(amount);
                addIfPresent(sellers, current.traderAddress());
            }
        }
        return new TradeMetrics(buyVolume, sellVolume, buyVolume.subtract(sellVolume),
                buyers.size(), sellers.size());
    }

    private void removeExpired(Deque<MarketTrade> trades, Instant now) {
        Instant cutoff = now.minus(WINDOW);
        while (!trades.isEmpty() && trades.peekFirst().observedAt().isBefore(cutoff)) {
            trades.removeFirst();
        }
    }

    private void addIfPresent(Set<String> addresses, String address) {
        if (address != null && !address.isBlank()) {
            addresses.add(address);
        }
    }

    public record TradeMetrics(
            BigDecimal buyVolume5m,
            BigDecimal sellVolume5m,
            BigDecimal netFlow5m,
            int uniqueBuyers5m,
            int uniqueSellers5m) {
    }
}
