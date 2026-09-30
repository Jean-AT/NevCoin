package com.trading.nevcoin.wallet.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletTrade(
        String walletAddress,
        String tokenAddress,
        Side side,
        BigDecimal amountToken,
        BigDecimal amountUsd,
        BigDecimal estimatedPrice,
        String transactionSignature,
        Instant timestamp) {
    public enum Side { BUY, SELL }
}
