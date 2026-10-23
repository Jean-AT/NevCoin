package com.trading.nevcoin.history.domain;

import java.time.Instant;
import java.util.Set;

public record ReplayRequest(Instant from, Instant to, Instant asOf, Set<String> tokenAddresses) {
    public ReplayRequest {
        if (from == null || to == null || asOf == null) throw new IllegalArgumentException("Replay times are required");
        if (to.isBefore(from)) throw new IllegalArgumentException("Replay end must not precede start");
        tokenAddresses = tokenAddresses == null ? Set.of() : Set.copyOf(tokenAddresses);
    }
}
