package com.trading.nevcoin.social.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A social observation only; it intentionally contains no trading recommendation. */
public record SocialSignal(
        String signalId,
        String tokenAddress,
        SocialSignalType signalType,
        BigDecimal strength,
        BigDecimal confidence,
        Instant firstObservedAt,
        Instant lastObservedAt,
        Instant expiresAt,
        int sourceCount,
        int uniqueSourceCount,
        List<SocialSignalEvidence> evidence,
        String algorithmVersion) {
    public SocialSignal {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        algorithmVersion = algorithmVersion == null ? "social-signal-v1" : algorithmVersion;
    }

    public boolean isActiveAt(Instant instant) {
        return expiresAt != null && !expiresAt.isBefore(instant);
    }
}
