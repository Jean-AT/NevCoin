package com.trading.nevcoin.notification.application.ports;

public interface NotificationPort {

    void send(Notification notification);

    record Notification(long chatId, String text) {
    }
}
