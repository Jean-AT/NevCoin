package com.trading.nevcoin.notification.application.ports;

import com.trading.nevcoin.notification.domain.NotificationOutboxEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxStore {
    /** Compatibility contract for generic notifications. */
    default NotificationOutboxEntry enqueue(String idempotencyKey, String channel, String recipient,
                                            String payload, Instant now) {
        throw new UnsupportedOperationException("This outbox adapter does not support enqueue");
    }

    default NotificationOutboxEntry enqueue(String idempotencyKey, String eventType, String channel,
                                            String recipient, String payload, Instant now) {
        return enqueue(idempotencyKey, channel, recipient, payload, now);
    }

    List<NotificationOutboxEntry> claimPending(Instant now, int limit, String claimToken, Instant leaseUntil);

    Optional<NotificationOutboxEntry> markDelivered(String id, String claimToken, Instant deliveredAt);

    Optional<NotificationOutboxEntry> markFailed(String id, String claimToken, Instant failedAt,
                                                  String error, Instant nextAttemptAt);
}
