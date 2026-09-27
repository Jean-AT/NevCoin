package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.domain.MarketSignal;
import com.trading.nevcoin.market.domain.MarketSnapshot;
import com.trading.nevcoin.market.domain.MarketTick;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class MarketSignalDetector {

    public List<MarketSignal> detect(
            MarketTick current,
            MarketSnapshot snapshot,
            MarketTick previous,
            MarketProperties properties) {
        List<MarketSignal> signals = new ArrayList<>();

        BigDecimal momentum = current.priceChange24hPercent();
        if (momentum != null && momentum.abs().compareTo(properties.getMomentumThresholdPercent()) >= 0) {
            signals.add(signal(
                    current,
                    MarketSignal.SignalType.MOMENTUM_DETECTED,
                    momentum.abs(),
                    "24h price change: " + momentum + "%"));
        }

        if (previous != null && previous.volume24hUsd() != null && previous.volume24hUsd().signum() > 0
                && current.volume24hUsd() != null
                && current.volume24hUsd().compareTo(previous.volume24hUsd()
                        .multiply(properties.getVolumeSpikeMultiplier())) >= 0) {
            signals.add(signal(
                    current,
                    MarketSignal.SignalType.VOLUME_SPIKE_DETECTED,
                    current.volume24hUsd().divide(previous.volume24hUsd(), 4, java.math.RoundingMode.HALF_UP),
                    "24h volume increased to " + current.volume24hUsd()));
        }

        if (previous != null && previous.liquidityUsd() != null && previous.liquidityUsd().signum() > 0
                && current.liquidityUsd() != null
                && current.liquidityUsd().compareTo(previous.liquidityUsd()
                        .multiply(BigDecimal.ONE.subtract(properties.getLiquidityDropThresholdPercent()
                                .divide(BigDecimal.valueOf(100), 8, java.math.RoundingMode.HALF_UP)))) <= 0) {
            signals.add(signal(
                    current,
                    MarketSignal.SignalType.LIQUIDITY_DROP_DETECTED,
                    properties.getLiquidityDropThresholdPercent(),
                    "Liquidity changed from " + previous.liquidityUsd() + " to " + current.liquidityUsd()));
        }

        return signals;
    }

    private MarketSignal signal(MarketTick tick, MarketSignal.SignalType type, BigDecimal strength, String evidence) {
        return new MarketSignal(
                tick.tokenAddress(),
                type,
                strength,
                tick.observedAt(),
                tick.observedAt().plus(Duration.ofMinutes(15)),
                List.of(evidence),
                tick.sourceQuality());
    }
}
