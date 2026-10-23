package com.trading.nevcoin.paper.validation.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Immutable signal input captured at detection time. */
public record ReplaySignal(
        String signalId,
        String tokenAddress,
        String signalType,
        Instant detectedAt,
        Instant expiresAt,
        List<String> evidence,
        String algorithmVersion) {
    public ReplaySignal {
        if (signalId == null || signalId.isBlank()) throw new IllegalArgumentException("signalId is required");
        if (tokenAddress == null || tokenAddress.isBlank()) throw new IllegalArgumentException("tokenAddress is required");
        if (detectedAt == null) throw new IllegalArgumentException("detectedAt is required");
        if (expiresAt != null && expiresAt.isBefore(detectedAt)) throw new IllegalArgumentException("expiresAt precedes detectedAt");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        algorithmVersion = algorithmVersion == null ? "unknown" : algorithmVersion;
    }
}
