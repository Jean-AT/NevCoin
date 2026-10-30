package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.application.ports.SocialProviderFailure;
import com.trading.nevcoin.social.application.ports.SocialSourceStateStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "social_source_states")
class SocialSourceStateEntity {
    @Id
    @Column(name = "source_id", length = 256)
    private String sourceId;
    private Instant lastAttemptAt;
    private Instant lastSuccessAt;
    private Instant lastFailureAt;
    private Instant nextAttemptAt;
    private int consecutiveFailures;
    @Column(length = 1024)
    private String lastError;
    private long lastLatencyMs;

    protected SocialSourceStateEntity() {
    }

    SocialSourceStateEntity(String sourceId) {
        this.sourceId = sourceId;
    }

    SocialSourceStateStore.State toState() {
        return new SocialSourceStateStore.State(sourceId, lastAttemptAt, lastSuccessAt, lastFailureAt,
                nextAttemptAt, consecutiveFailures, lastError, lastLatencyMs);
    }

    void markAttempt(Instant attemptedAt) {
        lastAttemptAt = attemptedAt;
    }

    void markSuccess(Instant completedAt, long latencyMs) {
        lastSuccessAt = completedAt;
        nextAttemptAt = null;
        consecutiveFailures = 0;
        lastError = null;
        lastLatencyMs = latencyMs;
    }

    void markFailure(Instant failedAt, SocialProviderFailure failure, Instant nextAttempt, long latencyMs) {
        lastFailureAt = failedAt;
        nextAttemptAt = nextAttempt;
        consecutiveFailures++;
        lastError = failure.category() + ": " + failure.message();
        lastLatencyMs = latencyMs;
    }
}
