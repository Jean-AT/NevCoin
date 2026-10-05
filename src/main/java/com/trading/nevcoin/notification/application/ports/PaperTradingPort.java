package com.trading.nevcoin.notification.application.ports;

import com.trading.nevcoin.trade.domain.PaperPosition;
import com.trading.nevcoin.trade.domain.PaperTrade;

import java.math.BigDecimal;
import java.util.List;

public interface PaperTradingPort {

    void setEnabled(boolean enabled);

    void reset();

    PaperStatus status();

    record PaperStatus(
            boolean enabled,
            BigDecimal cashUsd,
            List<PaperPosition> positions,
            List<PaperTrade> recentTrades) {
    }
}
