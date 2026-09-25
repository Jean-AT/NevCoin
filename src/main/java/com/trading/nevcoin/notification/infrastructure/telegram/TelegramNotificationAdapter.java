package com.trading.nevcoin.notification.infrastructure.telegram;

import com.trading.nevcoin.notification.application.ports.NotificationPort;
import org.springframework.stereotype.Component;

@Component
public class TelegramNotificationAdapter implements NotificationPort {

    private final TelegramApiClient telegramApiClient;

    public TelegramNotificationAdapter(TelegramApiClient telegramApiClient) {
        this.telegramApiClient = telegramApiClient;
    }

    @Override
    public void send(Notification notification) {
        telegramApiClient.sendMessage(notification.chatId(), notification.text());
    }
}
