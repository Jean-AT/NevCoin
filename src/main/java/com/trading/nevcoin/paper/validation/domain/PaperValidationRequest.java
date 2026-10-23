package com.trading.nevcoin.paper.validation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record PaperValidationRequest(
        Instant from,
        Instant to,
        BigDecimal initialCashUsd,
        BigDecimal tradeNotionalUsd,
        BigDecimal feePercent,
        int slippageBps,
        int maxOpenPositions,
        Set<String> signalTypes,
        Set<String> tokenAddresses,
        BigDecimal takeProfitPercent,
        BigDecimal stopLossPercent) {
    public PaperValidationRequest {
        if (from == null || to == null || to.isBefore(from)) throw new IllegalArgumentException("Invalid validation period");
        requireNonNegative(initialCashUsd, "initialCashUsd");
        requirePositive(tradeNotionalUsd, "tradeNotionalUsd");
        requireNonNegative(feePercent, "feePercent");
        if (slippageBps < 0 || maxOpenPositions < 1) throw new IllegalArgumentException("Invalid slippage or position limit");
        requireNonNegative(takeProfitPercent, "takeProfitPercent");
        requireNonNegative(stopLossPercent, "stopLossPercent");
        signalTypes = signalTypes == null ? Set.of() : Set.copyOf(signalTypes);
        tokenAddresses = tokenAddresses == null ? Set.of() : Set.copyOf(tokenAddresses);
    }

    public PaperValidationRequest(Instant from, Instant to, BigDecimal initialCashUsd, BigDecimal tradeNotionalUsd,
                                  BigDecimal feePercent, int slippageBps, int maxOpenPositions,
                                  Set<String> signalTypes, Set<String> tokenAddresses) {
        this(from, to, initialCashUsd, tradeNotionalUsd, feePercent, slippageBps, maxOpenPositions,
                signalTypes, tokenAddresses, BigDecimal.valueOf(25), BigDecimal.valueOf(10));
    }

    private static void requireNonNegative(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException(name + " must be non-negative");
    }
    private static void requirePositive(BigDecimal value, String name) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException(name + " must be positive");
    }
}
