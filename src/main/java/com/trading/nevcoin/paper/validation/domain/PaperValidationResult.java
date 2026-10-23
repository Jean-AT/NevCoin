package com.trading.nevcoin.paper.validation.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PaperValidationResult(
        BigDecimal initialCashUsd,
        BigDecimal finalCashUsd,
        BigDecimal totalReturnUsd,
        BigDecimal totalReturnPercent,
        BigDecimal realizedPnlUsd,
        BigDecimal unrealizedPnlUsd,
        BigDecimal maxDrawdownUsd,
        BigDecimal maxDrawdownPercent,
        int totalTrades,
        int winningTrades,
        int losingTrades,
        BigDecimal winRate,
        BigDecimal averageWinUsd,
        BigDecimal averageLossUsd,
        BigDecimal profitFactor,
        BigDecimal averageExposurePercent,
        BigDecimal averageHoldingMinutes,
        List<EquityPoint> equityCurve,
        List<PaperValidationTrade> tradeResults,
        Set<String> algorithmVersions,
        Map<String, BigDecimal> returnBySignalType,
        Map<String, BigDecimal> returnByToken) {
    public PaperValidationResult {
        equityCurve = equityCurve == null ? List.of() : List.copyOf(equityCurve);
        tradeResults = tradeResults == null ? List.of() : List.copyOf(tradeResults);
        algorithmVersions = algorithmVersions == null ? Set.of() : Set.copyOf(algorithmVersions);
        returnBySignalType = returnBySignalType == null ? Map.of() : Map.copyOf(returnBySignalType);
        returnByToken = returnByToken == null ? Map.of() : Map.copyOf(returnByToken);
    }
}
