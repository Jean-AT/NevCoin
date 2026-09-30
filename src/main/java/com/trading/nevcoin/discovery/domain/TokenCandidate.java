package com.trading.nevcoin.discovery.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TokenCandidate(
        String mintAddress,
        BigDecimal eligibilityScore,
        List<String> reasons,
        Instant observedAt,
        Instant expiresAt) {

    public TokenCandidate {
        reasons = List.copyOf(reasons);
    }
}
