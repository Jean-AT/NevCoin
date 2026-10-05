package com.trading.nevcoin.trade.application;

import com.trading.nevcoin.market.domain.MarketTick;
import com.trading.nevcoin.trade.domain.PaperPosition;
import com.trading.nevcoin.trade.domain.TradeDecision;

import java.util.Optional;

public interface DecisionEngine {

    TradeDecision evaluate(MarketTick tick, Optional<PaperPosition> position);
}
