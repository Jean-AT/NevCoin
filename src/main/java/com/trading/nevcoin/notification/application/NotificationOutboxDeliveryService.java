package com.trading.nevcoin.notification.application;

import com.trading.nevcoin.notification.application.ports.NotificationOutboxStore;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import com.trading.nevcoin.notification.domain.NotificationOutboxEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationOutboxDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxDeliveryService.class);
    private static final String TELEGRAM = "TELEGRAM";
    private final NotificationOutboxStore store;
    private final NotificationPort notifications;
    private final NotificationOutboxProperties properties;

    public NotificationOutboxDeliveryService(NotificationOutboxStore store, NotificationPort notifications,
                                             NotificationOutboxProperties properties) {
        this.store = store;
        this.notifications = notifications;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${notification.outbox.poll-interval:PT5S}")
    public void deliverPending() {
        if (!properties.isEnabled()) return;
        Instant now = Instant.now();
        String claimToken = UUID.randomUUID().toString();
        List<NotificationOutboxEntry> entries;
        try {
            entries = store.claimPending(now, properties.getBatchSize(), claimToken, now.plus(properties.getLease()));
        } catch (RuntimeException exception) {
            log.warn("Notification outbox claim failed", exception);
            return;
        }
        entries.forEach(entry -> deliver(entry, claimToken));
    }

    private void deliver(NotificationOutboxEntry entry, String claimToken) {
        try {
            if (!TELEGRAM.equalsIgnoreCase(entry.channel())) {
                fail(entry, claimToken, "Unsupported notification channel: " + entry.channel());
                return;
            }
            long chatId = Long.parseLong(entry.recipient());
            notifications.send(new NotificationPort.Notification(chatId, entry.payload()));
            store.markDelivered(entry.id(), claimToken, Instant.now());
        } catch (RuntimeException exception) {
            fail(entry, claimToken, exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private void fail(NotificationOutboxEntry entry, String claimToken, String error) {
        Instant now = Instant.now();
        Instant nextAttempt = entry.attempts() >= Math.max(1, properties.getMaxAttempts())
                ? Instant.MAX
                : now.plus(backoff(entry.attempts()));
        try {
            store.markFailed(entry.id(), claimToken, now, error, nextAttempt);
        } catch (RuntimeException persistenceFailure) {
            log.warn("Notification outbox failure update failed id={}", entry.id(), persistenceFailure);
        }
    }

    private Duration backoff(int attempts) {
        long multiplier = 1L << Math.min(Math.max(0, attempts - 1), 20);
        Duration candidate = properties.getBaseBackoff().multipliedBy(multiplier);
        return candidate.compareTo(properties.getMaxBackoff()) > 0 ? properties.getMaxBackoff() : candidate;
    }
}
