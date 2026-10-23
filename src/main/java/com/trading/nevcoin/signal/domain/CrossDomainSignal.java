package com.trading.nevcoin.signal.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Correlated intelligence; this is descriptive and is never an execution instruction. */
public record CrossDomainSignal(
        String signalId,
        String tokenAddress,
        SignalPriority priority,
        BigDecimal strength,
        BigDecimal confidence,
        Instant detectedAt,
        Instant expiresAt,
        Set<SignalComponent> components,
        List<SignalEvidence> evidence,
        String algorithmVersion) {
    public CrossDomainSignal {
        components = components == null ? Set.of() : Set.copyOf(components);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        algorithmVersion = algorithmVersion == null ? "cross-signal-v1" : algorithmVersion;
    }

    public boolean isActiveAt(Instant instant) { return !expiresAt.isBefore(instant); }
}
