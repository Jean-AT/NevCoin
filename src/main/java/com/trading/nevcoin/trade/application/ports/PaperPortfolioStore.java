package com.trading.nevcoin.trade.application.ports;

import com.trading.nevcoin.trade.domain.PaperPosition;
import com.trading.nevcoin.trade.domain.PaperTrade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaperPortfolioStore {

    boolean enabled(boolean defaultEnabled, BigDecimal initialCashUsd);

    void setEnabled(boolean enabled, BigDecimal initialCashUsd);

    BigDecimal cashBalance(BigDecimal initialCashUsd);

    List<PaperPosition> positions();

    Optional<PaperPosition> position(String tokenAddress);

    List<PaperTrade> recentTrades(int limit);

    BigDecimal realizedPnlSince(Instant since);

    void buy(PaperTrade trade);

    void sell(PaperTrade trade);

    void reset(BigDecimal initialCashUsd);
}
