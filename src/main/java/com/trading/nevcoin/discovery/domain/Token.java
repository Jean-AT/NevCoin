package com.trading.nevcoin.discovery.domain;

import java.time.Instant;

public record Token(
        String mintAddress,
        String symbol,
        String name,
        int decimals,
        Instant createdAt,
        Instant firstSeenAt,
        Status status) {

    public enum Status { ACTIVE, PAUSED }
}
