package com.trading.nevcoin.notification.application;

import com.trading.nevcoin.notification.application.ports.NotificationOutboxStore;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import com.trading.nevcoin.notification.domain.NotificationOutboxEntry;
import com.trading.nevcoin.notification.domain.NotificationOutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationOutboxDeliveryServiceTest {
    @Test
    void deliversClaimedTelegramMessageAndMarksItDelivered() {
        FakeStore store = new FakeStore(entry(0, NotificationOutboxStatus.PENDING));
        var sent = new java.util.ArrayList<NotificationPort.Notification>();
        var service = new NotificationOutboxDeliveryService(store, sent::add, properties(5));

        service.deliverPending();

        assertEquals(1, sent.size());
        assertEquals(123L, sent.getFirst().chatId());
        assertEquals(1, store.delivered);
        assertEquals(0, store.failed);
    }

    @Test
    void retriesDeliveryFailureWithBackoffAndDoesNotThrow() {
        FakeStore store = new FakeStore(entry(5, NotificationOutboxStatus.FAILED));
        var service = new NotificationOutboxDeliveryService(store, ignored -> { throw new IllegalStateException("telegram down"); }, properties(5));

        service.deliverPending();

        assertEquals(0, store.delivered);
        assertEquals(1, store.failed);
        assertEquals("telegram down", store.error);
        assertEquals(Instant.MAX, store.nextAttemptAt);
    }

    private NotificationOutboxProperties properties(int maxAttempts) {
        NotificationOutboxProperties properties = new NotificationOutboxProperties();
        properties.setBatchSize(10);
        properties.setLease(Duration.ofSeconds(30));
        properties.setBaseBackoff(Duration.ofSeconds(1));
        properties.setMaxBackoff(Duration.ofSeconds(10));
        properties.setMaxAttempts(maxAttempts);
        return properties;
    }

    private NotificationOutboxEntry entry(int attempts, NotificationOutboxStatus status) {
        Instant now = Instant.now();
        return new NotificationOutboxEntry("id-1", "key-1", null, "TELEGRAM", "123", "hello", status,
                attempts, now, null, now, now, null, null, null);
    }

    private static final class FakeStore implements NotificationOutboxStore {
        private final NotificationOutboxEntry entry;
        private int delivered;
        private int failed;
        private String error;
        private Instant nextAttemptAt;

        private FakeStore(NotificationOutboxEntry entry) { this.entry = entry; }
        @Override public NotificationOutboxEntry enqueue(String idempotencyKey, String channel, String recipient, String payload, Instant now) { return entry; }
        @Override public NotificationOutboxEntry enqueue(String idempotencyKey, String eventType, String channel, String recipient, String payload, Instant now) { return entry; }
        @Override public List<NotificationOutboxEntry> claimPending(Instant now, int limit, String claimToken, Instant leaseUntil) { return List.of(entry); }
        @Override public Optional<NotificationOutboxEntry> markDelivered(String id, String claimToken, Instant deliveredAt) { delivered++; return Optional.of(entry); }
        @Override public Optional<NotificationOutboxEntry> markFailed(String id, String claimToken, Instant failedAt, String error, Instant nextAttemptAt) {
            failed++; this.error = error; this.nextAttemptAt = nextAttemptAt; return Optional.of(entry);
        }
    }
}
