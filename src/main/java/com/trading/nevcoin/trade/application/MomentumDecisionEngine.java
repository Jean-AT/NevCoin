package com.trading.nevcoin.trade.application;

import com.trading.nevcoin.market.domain.MarketTick;
import com.trading.nevcoin.trade.domain.PaperPosition;
import com.trading.nevcoin.trade.domain.TradeAction;
import com.trading.nevcoin.trade.domain.TradeDecision;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class MomentumDecisionEngine implements DecisionEngine {

    private final PaperTradingProperties properties;

    public MomentumDecisionEngine(PaperTradingProperties properties) {
        this.properties = properties;
    }

    @Override
    public TradeDecision evaluate(MarketTick tick, Optional<PaperPosition> position) {
        BigDecimal momentum = tick.priceChange24hPercent();
        if (momentum == null) {
            return decision(tick, TradeAction.HOLD, BigDecimal.ZERO, "No 24h momentum available");
        }
        if (position.isEmpty() && momentum.compareTo(properties.getEntryMomentumThresholdPercent()) >= 0) {
            return decision(tick, TradeAction.BUY, confidence(momentum),
                    "24h momentum reached " + momentum + "%");
        }
        if (position.isPresent() && momentum.compareTo(properties.getExitMomentumThresholdPercent()) <= 0) {
            return decision(tick, TradeAction.SELL, confidence(momentum.abs()),
                    "24h momentum fell to " + momentum + "%");
        }
        return decision(tick, TradeAction.HOLD, BigDecimal.ZERO, "Momentum is below the configured action threshold");
    }

    private TradeDecision decision(MarketTick tick, TradeAction action, BigDecimal confidence, String evidence) {
        return new TradeDecision(tick.tokenAddress(), action, confidence, tick.observedAt(), List.of(evidence));
    }

    private BigDecimal confidence(BigDecimal momentum) {
        return momentum.min(BigDecimal.valueOf(100)).max(BigDecimal.ZERO);
    }
}
