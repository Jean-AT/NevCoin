package com.trading.nevcoin.wallet.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

public record WalletProfile(
        String walletAddress,
        int closedTrades,
        BigDecimal winRate,
        BigDecimal realizedPnl,
        Duration averageHoldingTime,
        int sampleSize,
        BigDecimal confidence,
        Instant lastCalculatedAt) { }
