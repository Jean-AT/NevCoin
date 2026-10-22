package com.trading.nevcoin.social.application.ports;

import com.trading.nevcoin.social.domain.RawSocialEvent;

import java.time.Instant;
import java.util.List;

public record SocialFetchResult(
        List<RawSocialEvent> events,
        String nextCursor,
        Instant fetchedAt,
        List<SocialProviderFailure> failures) {
    public SocialFetchResult {
        events = events == null ? List.of() : List.copyOf(events);
        failures = failures == null ? List.of() : List.copyOf(failures);
        fetchedAt = fetchedAt == null ? Instant.now() : fetchedAt;
    }
}
