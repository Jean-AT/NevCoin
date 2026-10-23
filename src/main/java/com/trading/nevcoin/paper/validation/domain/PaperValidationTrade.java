package com.trading.nevcoin.paper.validation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaperValidationTrade(
        String signalId,
        String tokenAddress,
        Instant detectedAt,
        BigDecimal entryPrice,
        BigDecimal exitPrice,
        BigDecimal quantity,
        BigDecimal entryNotionalUsd,
        BigDecimal exitNotionalUsd,
        BigDecimal feesUsd,
        BigDecimal slippageUsd,
        BigDecimal realizedPnlUsd,
        String exitReason,
        List<String> evidence) {
    public PaperValidationTrade {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
    public boolean isOpen() { return exitPrice == null; }
}
