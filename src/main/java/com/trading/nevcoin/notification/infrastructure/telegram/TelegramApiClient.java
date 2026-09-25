package com.trading.nevcoin.notification.infrastructure.telegram;

import java.util.List;

public interface TelegramApiClient {

    List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds);

    void sendMessage(long chatId, String text);

    record TelegramUpdate(long updateId, long chatId, String text) {
    }
}
