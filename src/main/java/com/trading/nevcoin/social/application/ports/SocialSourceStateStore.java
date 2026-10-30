package com.trading.nevcoin.social.application.ports;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Durable operational state for each configured social source. */
public interface SocialSourceStateStore {
    Optional<State> find(String sourceId);

    void markAttempt(String sourceId, Instant attemptedAt);

    void markSuccess(String sourceId, Instant completedAt, long latencyMs);

    void markFailure(String sourceId, Instant failedAt, SocialProviderFailure failure, Duration backoff,
                     long latencyMs);

    List<State> list();

    record State(String sourceId, Instant lastAttemptAt, Instant lastSuccessAt, Instant lastFailureAt,
                 Instant nextAttemptAt, int consecutiveFailures, String lastError, long lastLatencyMs) {
    }
}
