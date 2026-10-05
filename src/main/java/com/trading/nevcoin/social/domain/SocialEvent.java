package com.trading.nevcoin.social.domain;

import java.time.Instant;
import java.util.List;

/** Normalized, descriptive social mention associated with an optional token. */
public record SocialEvent(
        String source,
        String externalId,
        String tokenAddress,
        String author,
        String text,
        Instant observedAt,
        List<String> evidence) {
    public SocialEvent {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
