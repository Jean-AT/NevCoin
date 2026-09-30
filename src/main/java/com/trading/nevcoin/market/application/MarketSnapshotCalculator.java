package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.domain.MarketSnapshot;
import com.trading.nevcoin.market.domain.MarketTick;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class MarketSnapshotCalculator {

    public MarketSnapshot calculate(MarketTick current, List<MarketTick> previousTicks) {
        BigDecimal priceChange1m = priceChangeAt(current, previousTicks, Duration.ofMinutes(1));
        BigDecimal priceChange5m = priceChangeAt(current, previousTicks, Duration.ofMinutes(5));
        BigDecimal priceChange15m = priceChangeAt(current, previousTicks, Duration.ofMinutes(15));
        BigDecimal volatility5m = volatility(current, previousTicks, Duration.ofMinutes(5));
        long freshness = Math.max(0, Duration.between(current.sourceTimestamp(), current.observedAt()).toMillis());

        return new MarketSnapshot(
                current.tokenAddress(),
                current.observedAt(),
                current.priceUsd(),
                current.liquidityUsd(),
                current.volume24hUsd(),
                priceChange1m,
                priceChange5m,
                priceChange15m,
                volatility5m,
                freshness,
                current.buyVolume5m(),
                current.sellVolume5m(),
                current.netFlow5m(),
                current.uniqueBuyers5m(),
                current.uniqueSellers5m());
    }

    private BigDecimal priceChangeAt(MarketTick current, List<MarketTick> previousTicks, Duration window) {
        Instant cutoff = current.observedAt().minus(window);
        return previousTicks.stream()
                .filter(tick -> !tick.observedAt().isAfter(cutoff))
                .findFirst()
                .map(tick -> percentageChange(tick.priceUsd(), current.priceUsd()))
                .orElse(null);
    }

    private BigDecimal volatility(MarketTick current, List<MarketTick> previousTicks, Duration window) {
        Instant cutoff = current.observedAt().minus(window);
        List<BigDecimal> prices = previousTicks.stream()
                .filter(tick -> !tick.observedAt().isBefore(cutoff))
                .map(MarketTick::priceUsd)
                .filter(price -> price != null && price.signum() > 0)
                .toList();
        if (prices.isEmpty() || current.priceUsd() == null) {
            return null;
        }
        BigDecimal minimum = prices.stream().min(BigDecimal::compareTo).orElse(current.priceUsd());
        BigDecimal maximum = prices.stream().max(BigDecimal::compareTo).orElse(current.priceUsd());
        return percentageChange(minimum, maximum).abs();
    }

    private BigDecimal percentageChange(BigDecimal from, BigDecimal to) {
        if (from == null || to == null || from.signum() == 0) {
            return null;
        }
        return to.subtract(from)
                .divide(from, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
