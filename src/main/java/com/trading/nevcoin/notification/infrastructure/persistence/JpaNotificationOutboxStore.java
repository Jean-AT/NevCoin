package com.trading.nevcoin.notification.infrastructure.persistence;

import com.trading.nevcoin.notification.application.ports.NotificationOutboxStore;
import com.trading.nevcoin.notification.domain.NotificationOutboxEntry;
import com.trading.nevcoin.notification.domain.NotificationOutboxStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaNotificationOutboxStore implements NotificationOutboxStore {
    private final NotificationOutboxJpaRepository repository;

    public JpaNotificationOutboxStore(NotificationOutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public NotificationOutboxEntry enqueue(String idempotencyKey, String channel, String recipient,
                                           String payload, Instant now) {
        return enqueue(idempotencyKey, null, channel, recipient, payload, now);
    }

    @Override
    @Transactional
    public NotificationOutboxEntry enqueue(String idempotencyKey, String eventType, String channel, String recipient,
                                           String payload, Instant now) {
        repository.insertIfAbsent(UUID.randomUUID().toString(), idempotencyKey, eventType, channel, recipient, payload, now);
        return repository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Notification outbox insert was not durable"))
                .toDomain();
    }

    @Override
    @Transactional
    public List<NotificationOutboxEntry> claimPending(Instant now, int limit, String claimToken, Instant leaseUntil) {
        if (limit <= 0) return List.of();
        List<NotificationOutboxEntity> entries = repository.findClaimable(
                List.of(NotificationOutboxStatus.PENDING, NotificationOutboxStatus.FAILED), now,
                PageRequest.of(0, limit));
        entries.forEach(entry -> entry.claim(claimToken, leaseUntil, now));
        repository.saveAll(entries);
        return entries.stream().map(NotificationOutboxEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public Optional<NotificationOutboxEntry> markDelivered(String id, String claimToken, Instant deliveredAt) {
        if (repository.markDelivered(id, claimToken, NotificationOutboxStatus.DELIVERED, deliveredAt, deliveredAt) == 0) {
            return Optional.empty();
        }
        return repository.findById(id).map(NotificationOutboxEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<NotificationOutboxEntry> markFailed(String id, String claimToken, Instant failedAt,
                                                        String error, Instant nextAttemptAt) {
        if (repository.markFailed(id, claimToken, NotificationOutboxStatus.FAILED, error, nextAttemptAt, failedAt) == 0) {
            return Optional.empty();
        }
        return repository.findById(id).map(NotificationOutboxEntity::toDomain);
    }
}
