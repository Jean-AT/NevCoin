package com.trading.nevcoin.notification.infrastructure;

import com.trading.nevcoin.notification.application.NotificationOutboxProperties;
import com.trading.nevcoin.notification.application.ports.NotificationOutboxStore;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import com.trading.nevcoin.notification.infrastructure.telegram.TelegramProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Makes every application notification durable before it reaches Telegram.
 * The low-level adapter is injected explicitly into the outbox worker so this
 * adapter cannot recursively enqueue its own deliveries.
 */
@Primary
@Component
public class OutboxNotificationAdapter implements NotificationPort {
    private static final String TELEGRAM = "TELEGRAM";

    private final NotificationOutboxStore outbox;
    private final NotificationOutboxProperties outboxProperties;
    private final TelegramProperties telegramProperties;
    private final NotificationPort directNotifications;

    public OutboxNotificationAdapter(NotificationOutboxStore outbox,
                                     NotificationOutboxProperties outboxProperties,
                                     TelegramProperties telegramProperties,
                                     @Qualifier("telegramNotificationPort") NotificationPort directNotifications) {
        this.outbox = outbox;
        this.outboxProperties = outboxProperties;
        this.telegramProperties = telegramProperties;
        this.directNotifications = directNotifications;
    }

    @Override
    public void send(Notification notification) {
        if (!outboxProperties.isEnabled() || !telegramProperties.isEnabled()) {
            directNotifications.send(notification);
            return;
        }
        outbox.enqueue(
                "notification:" + UUID.randomUUID(),
                "TELEGRAM_NOTIFICATION",
                TELEGRAM,
                Long.toString(notification.chatId()),
                notification.text(),
                Instant.now());
    }
}
