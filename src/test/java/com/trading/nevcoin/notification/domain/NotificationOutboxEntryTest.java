package com.trading.nevcoin.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationOutboxEntryTest {
    @Test
    void defaultsPendingStatusAndTimestamps() {
        Instant now = Instant.parse("2026-10-24T00:00:00Z");
        var entry = new NotificationOutboxEntry("id", "key", "SIGNAL", "telegram", "123", "hello",
                null, 0, null, null, now, null, null, null, null);

        assertEquals(NotificationOutboxStatus.PENDING, entry.status());
        assertEquals(now, entry.nextAttemptAt());
        assertEquals(now, entry.updatedAt());
    }

    @Test
    void rejectsMissingIdempotencyKey() {
        assertThrows(IllegalArgumentException.class, () -> new NotificationOutboxEntry(
                "id", "", "SIGNAL", "telegram", "123", "hello", NotificationOutboxStatus.PENDING,
                0, Instant.now(), null, Instant.now(), Instant.now(), null, null, null));
    }
}
