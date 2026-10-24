package com.trading.nevcoin.signal.infrastructure;

import com.trading.nevcoin.notification.application.ports.NotificationOutboxStore;
import com.trading.nevcoin.signal.application.ports.SignalPublishedEventPublisher;
import com.trading.nevcoin.signal.domain.CrossDomainSignal;
import com.trading.nevcoin.notification.infrastructure.telegram.TelegramProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OutboxSignalPublishedEventPublisher implements SignalPublishedEventPublisher {
    private final NotificationOutboxStore outbox;
    private final TelegramProperties telegramProperties;

    public OutboxSignalPublishedEventPublisher(NotificationOutboxStore outbox, TelegramProperties telegramProperties) {
        this.outbox = outbox;
        this.telegramProperties = telegramProperties;
    }

    @Override
    public void publish(CrossDomainSignal signal) {
        if (!telegramProperties.isEnabled()) return;
        String payload = format(signal);
        for (Long chatId : telegramProperties.parsedAllowedChatIds()) {
            outbox.enqueue("cross-signal:" + signal.signalId() + ":" + chatId,
                    "CROSS_DOMAIN_SIGNAL", "TELEGRAM", chatId.toString(), payload, Instant.now());
        }
    }

    private String format(CrossDomainSignal signal) {
        return "⚡ CROSS SIGNAL\n\n"
                + "Token: " + signal.tokenAddress() + "\n"
                + "Priority: " + signal.priority() + "\n"
                + "Strength: " + signal.strength() + "\n"
                + "Confidence: " + signal.confidence() + "\n"
                + "Detected: " + signal.detectedAt() + "\n"
                + "Components: " + signal.components() + "\n"
                + "Evidence: " + signal.evidence() + "\n\n"
                + "Descriptive intelligence only. No trade was executed.";
    }
}
