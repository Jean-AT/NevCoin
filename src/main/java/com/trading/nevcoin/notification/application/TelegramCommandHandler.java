package com.trading.nevcoin.notification.application;

import com.trading.nevcoin.notification.application.ports.SystemStatusPort;
import com.trading.nevcoin.notification.domain.TelegramAccessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class TelegramCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramCommandHandler.class);

    private static final String HELP_MESSAGE = """
            NevCoin commands:
            /status - show application status
            /health - show health status
            /help - show this help

            NevCoin is paper intelligence only. Trading commands are disabled.
            """;

    private final TelegramAccessPolicy accessPolicy;
    private final SystemStatusPort systemStatusPort;

    public TelegramCommandHandler(TelegramAccessPolicy accessPolicy, SystemStatusPort systemStatusPort) {
        this.accessPolicy = accessPolicy;
        this.systemStatusPort = systemStatusPort;
    }

    public Optional<String> handle(long chatId, String text) {
        if (!accessPolicy.isAllowed(chatId)) {
            log.warn("Ignoring Telegram command from unauthorized chatId={}", chatId);
            return Optional.empty();
        }

        String command = normalizeCommand(text);
        return switch (command) {
            case "/status" -> Optional.of(statusMessage());
            case "/health" -> Optional.of(healthMessage());
            case "/help" -> Optional.of(HELP_MESSAGE);
            case "" -> Optional.of("Use /help to list available commands.");
            default -> Optional.of("Unknown command.\n\n" + HELP_MESSAGE);
        };
    }

    private String normalizeCommand(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String firstToken = text.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int mentionSeparator = firstToken.indexOf('@');
        return mentionSeparator >= 0 ? firstToken.substring(0, mentionSeparator) : firstToken;
    }

    private String statusMessage() {
        SystemStatusPort.SystemStatus status = systemStatusPort.current();
        return "NevCoin status\nMode: %s\nHealth: %s\nTrading: disabled"
                .formatted(status.mode(), status.health());
    }

    private String healthMessage() {
        SystemStatusPort.SystemStatus status = systemStatusPort.current();
        return "Health: %s\nTrading: disabled".formatted(status.health());
    }
}
