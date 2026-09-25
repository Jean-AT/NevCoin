package com.trading.nevcoin.notification.application;

import com.trading.nevcoin.notification.application.ports.SystemStatusPort;
import com.trading.nevcoin.notification.domain.TelegramAccessPolicy;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelegramCommandHandlerTest {

    private static final long ALLOWED_CHAT_ID = 123L;

    private final TelegramCommandHandler handler = new TelegramCommandHandler(
            new TelegramAccessPolicy(Set.of(ALLOWED_CHAT_ID)),
            () -> new SystemStatusPort.SystemStatus("UP", "paper-intelligence"));

    @Test
    void rejectsUnauthorizedChat() {
        assertTrue(handler.handle(999L, "/help").isEmpty());
    }

    @Test
    void returnsHelpForHelpCommand() {
        assertTrue(handler.handle(ALLOWED_CHAT_ID, "/help").orElseThrow().contains("/status"));
    }

    @Test
    void supportsBotMentionOnCommand() {
        assertEquals("Health: UP\nTrading: disabled", handler.handle(ALLOWED_CHAT_ID, "/health@nevcoin_bot").orElseThrow());
    }

    @Test
    void unknownCommandReturnsHelp() {
        assertTrue(handler.handle(ALLOWED_CHAT_ID, "/buy TOKEN").orElseThrow().contains("Unknown command"));
    }
}
