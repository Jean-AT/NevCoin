package com.trading.nevcoin.notification.domain;

import java.time.Instant;

/** Durable, provider-neutral notification delivery work item. */
public record NotificationOutboxEntry(
        String id,
        String idempotencyKey,
        String eventType,
        String channel,
        String recipient,
        String payload,
        NotificationOutboxStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt,
        Instant deliveredAt,
        Instant claimedUntil,
        String claimToken) {
    /** Compatibility constructor for providers created before event type was persisted. */
    public NotificationOutboxEntry(String id, String idempotencyKey, String channel, String recipient,
                                   String payload, NotificationOutboxStatus status, int attempts,
                                   Instant nextAttemptAt, String lastError, Instant createdAt, Instant updatedAt,
                                   Instant deliveredAt, Instant claimedUntil, String claimToken) {
        this(id, idempotencyKey, null, channel, recipient, payload, status, attempts, nextAttemptAt,
                lastError, createdAt, updatedAt, deliveredAt, claimedUntil, claimToken);
    }

    public NotificationOutboxEntry {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Outbox id is required");
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("Idempotency key is required");
        if (channel == null || channel.isBlank()) throw new IllegalArgumentException("Channel is required");
        if (recipient == null || recipient.isBlank()) throw new IllegalArgumentException("Recipient is required");
        if (payload == null) throw new IllegalArgumentException("Payload is required");
        if (status == null) status = NotificationOutboxStatus.PENDING;
        if (attempts < 0) throw new IllegalArgumentException("Attempts cannot be negative");
        if (nextAttemptAt == null) nextAttemptAt = createdAt == null ? Instant.now() : createdAt;
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }
}
