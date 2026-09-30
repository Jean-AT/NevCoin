package com.trading.nevcoin.wallet.application;

import com.trading.nevcoin.wallet.domain.WalletProfile;
import com.trading.nevcoin.wallet.domain.WalletTrade;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class WalletProfileCalculator {
    private static final int MINIMUM_SAMPLE = 5;

    public WalletProfile calculate(String walletAddress, List<WalletTrade> trades, Instant calculatedAt) {
        int closed = Math.min(trades.size() / 2, trades.size());
        int wins = trades.stream().filter(trade -> trade.side() == WalletTrade.Side.SELL
                && trade.amountUsd() != null && trade.amountUsd().signum() > 0).toList().size();
        BigDecimal winRate = closed == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(wins)
                .divide(BigDecimal.valueOf(closed), 4, RoundingMode.HALF_UP);
        BigDecimal confidence = BigDecimal.valueOf(Math.min(1.0, (double) trades.size() / MINIMUM_SAMPLE))
                .setScale(2, RoundingMode.HALF_UP);
        return new WalletProfile(walletAddress, closed, winRate, BigDecimal.ZERO,
                Duration.ZERO, trades.size(), confidence, calculatedAt);
    }
}
