package com.trading.nevcoin.discovery.application;

import com.trading.nevcoin.discovery.domain.TokenCandidate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TokenEligibilityScorer {

    private final DiscoveryProperties properties;

    public TokenEligibilityScorer(DiscoveryProperties properties) {
        this.properties = properties;
    }

    public TokenCandidate evaluate(TokenMarketData data, Instant observedAt) {
        List<String> reasons = new ArrayList<>();
        int passed = 0;
        if (atLeast(data.liquidityUsd(), properties.getMinimumLiquidityUsd())) {
            passed++;
            reasons.add("liquidity above configured minimum");
        }
        if (atLeast(data.volume24hUsd(), properties.getMinimumVolume24hUsd())) {
            passed++;
            reasons.add("24h volume above configured minimum");
        }
        if (data.uniqueTraders5m() >= properties.getMinimumUniqueTraders5m()) {
            passed++;
            reasons.add("recent trader activity above configured minimum");
        }
        BigDecimal score = BigDecimal.valueOf(passed).movePointRight(2)
                .divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);
        return new TokenCandidate(data.mintAddress(), score, reasons, observedAt,
                observedAt.plus(Duration.ofMinutes(properties.getCandidateTtlMinutes())));
    }

    private boolean atLeast(BigDecimal actual, BigDecimal minimum) {
        return actual != null && actual.compareTo(minimum) >= 0;
    }

    public record TokenMarketData(
            String mintAddress,
            BigDecimal liquidityUsd,
            BigDecimal volume24hUsd,
            int uniqueTraders5m) { }
}
