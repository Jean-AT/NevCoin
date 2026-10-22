package com.trading.nevcoin.social.domain;

import java.time.Instant;
import java.util.Map;

/** Provider-neutral input captured before normalization or interpretation. */
public record RawSocialEvent(
        String provider,
        String sourceId,
        String sourceAccountId,
        String externalEventId,
        String canonicalUrl,
        Instant publishedAt,
        Instant receivedAt,
        String rawText,
        Map<String, Object> metadata) {
    public RawSocialEvent {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }
}
