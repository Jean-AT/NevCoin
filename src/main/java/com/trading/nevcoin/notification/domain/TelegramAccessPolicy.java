package com.trading.nevcoin.notification.domain;

import java.util.Set;

public record TelegramAccessPolicy(Set<Long> allowedChatIds) {

    public TelegramAccessPolicy {
        allowedChatIds = Set.copyOf(allowedChatIds);
    }

    public boolean isAllowed(long chatId) {
        return allowedChatIds.contains(chatId);
    }
}
