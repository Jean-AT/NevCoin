package com.trading.nevcoin.notification.application;

import com.trading.nevcoin.notification.application.ports.SystemStatusPort;
import com.trading.nevcoin.notification.application.ports.TokenQueryPort;
import com.trading.nevcoin.notification.application.ports.TokenWatchlistCommandPort;
import com.trading.nevcoin.notification.domain.TelegramAccessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

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
            /tokens - list watched tokens
            /token <mint|symbol> - show a watched token
            /watch-token <mint> - add a token to the watchlist
            /unwatch-token <mint> - remove a token from the watchlist

            NevCoin is paper intelligence only. Trading commands are disabled.
            """;

    private final TelegramAccessPolicy accessPolicy;
    private final SystemStatusPort systemStatusPort;
    private final TokenQueryPort tokenQueryPort;
    private final TokenWatchlistCommandPort tokenWatchlistCommandPort;

    public TelegramCommandHandler(TelegramAccessPolicy accessPolicy, SystemStatusPort systemStatusPort) {
        this(accessPolicy, systemStatusPort, new TokenQueryPort() {
            @Override public java.util.List<TokenQueryPort.TokenSummary> list() { return java.util.List.of(); }
            @Override public TokenQueryPort.TokenSummary find(String query) { return null; }
        }, new TokenWatchlistCommandPort() {
            @Override public void watch(String mintAddress) { }
            @Override public void unwatch(String mintAddress) { }
        });
    }

    @Autowired
    public TelegramCommandHandler(
            TelegramAccessPolicy accessPolicy,
            SystemStatusPort systemStatusPort,
            TokenQueryPort tokenQueryPort,
            TokenWatchlistCommandPort tokenWatchlistCommandPort) {
        this.accessPolicy = accessPolicy;
        this.systemStatusPort = systemStatusPort;
        this.tokenQueryPort = tokenQueryPort;
        this.tokenWatchlistCommandPort = tokenWatchlistCommandPort;
    }

    public Optional<String> handle(long chatId, String text) {
        if (!accessPolicy.isAllowed(chatId)) {
            log.warn("Ignoring Telegram command from unauthorized chatId={}", chatId);
            return Optional.empty();
        }

        String[] parts = text == null ? new String[0] : text.trim().split("\\s+");
        String command = normalizeCommand(parts.length == 0 ? "" : parts[0]);
        return switch (command) {
            case "/status" -> Optional.of(statusMessage());
            case "/health" -> Optional.of(healthMessage());
            case "/help" -> Optional.of(HELP_MESSAGE);
            case "/tokens" -> Optional.of(tokensMessage());
            case "/token" -> Optional.of(tokenMessage(argument(parts)));
            case "/watch-token" -> Optional.of(watchToken(argument(parts)));
            case "/unwatch-token" -> Optional.of(unwatchToken(argument(parts)));
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

    private String tokensMessage() {
        var tokens = tokenQueryPort.list();
        if (tokens.isEmpty()) return "No watched tokens.";
        return "Watched tokens\n" + tokens.stream()
                .map(token -> "- %s (%s) [%s]".formatted(
                        token.symbol() == null ? token.mintAddress() : token.symbol(), token.mintAddress(), token.status()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String tokenMessage(String query) {
        if (query.isBlank()) return "Usage: /token <mint|symbol>";
        var token = tokenQueryPort.find(query);
        return token == null ? "Token is not in the watchlist." :
                "Token\nMint: %s\nSymbol: %s\nName: %s\nStatus: %s".formatted(
                        token.mintAddress(), value(token.symbol()), value(token.name()), token.status());
    }

    private String watchToken(String mint) {
        if (mint.isBlank()) return "Usage: /watch-token <mint>";
        try { tokenWatchlistCommandPort.watch(mint); return "Token added to watchlist: " + mint; }
        catch (IllegalArgumentException exception) { return "Invalid token mint: " + exception.getMessage(); }
    }

    private String unwatchToken(String mint) {
        if (mint.isBlank()) return "Usage: /unwatch-token <mint>";
        try { tokenWatchlistCommandPort.unwatch(mint); return "Token removed from watchlist: " + mint; }
        catch (IllegalArgumentException exception) { return "Invalid token mint: " + exception.getMessage(); }
    }

    private String argument(String[] parts) { return parts.length < 2 ? "" : parts[1]; }
    private String value(String value) { return value == null ? "-" : value; }
}
