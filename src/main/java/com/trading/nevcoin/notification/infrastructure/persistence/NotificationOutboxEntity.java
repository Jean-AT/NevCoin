package com.trading.nevcoin.notification.infrastructure.persistence;

import com.trading.nevcoin.notification.domain.NotificationOutboxEntry;
import com.trading.nevcoin.notification.domain.NotificationOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutboxEntity {
    @Id
    private String id;
    @Column(name = "idempotency_key", nullable = false, length = 256, unique = true)
    private String idempotencyKey;
    @Column(name = "event_type", length = 128)
    private String eventType;
    @Column(nullable = false, length = 64)
    private String channel;
    @Column(nullable = false, length = 256)
    private String recipient;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationOutboxStatus status;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "claimed_until")
    private Instant claimedUntil;
    @Column(name = "claim_token", length = 128)
    private String claimToken;

    protected NotificationOutboxEntity() { }

    public NotificationOutboxEntity(String id, String idempotencyKey, String eventType, String channel, String recipient,
                                    String payload, Instant now) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.eventType = eventType;
        this.channel = channel;
        this.recipient = recipient;
        this.payload = payload;
        this.status = NotificationOutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public NotificationOutboxEntry toDomain() {
        return new NotificationOutboxEntry(id, idempotencyKey, eventType, channel, recipient, payload, status, attempts,
                nextAttemptAt, lastError, createdAt, updatedAt, deliveredAt, claimedUntil, claimToken);
    }

    public void claim(String token, Instant until, Instant now) {
        status = NotificationOutboxStatus.PENDING;
        claimToken = token;
        claimedUntil = until;
        attempts++;
        updatedAt = now;
    }

    public void delivered(Instant now) {
        status = NotificationOutboxStatus.DELIVERED;
        deliveredAt = now;
        claimedUntil = null;
        claimToken = null;
        updatedAt = now;
    }

    public void failed(String token, Instant now, String error, Instant retryAt) {
        if (claimToken != null && !claimToken.equals(token)) return;
        status = NotificationOutboxStatus.FAILED;
        lastError = error;
        nextAttemptAt = retryAt;
        claimedUntil = null;
        claimToken = null;
        updatedAt = now;
    }
}
