package com.trading.nevcoin.notification.application;

import com.trading.nevcoin.notification.application.ports.SystemStatusPort;
import com.trading.nevcoin.notification.application.ports.TokenMarketDataPort;
import com.trading.nevcoin.notification.application.ports.TokenQueryPort;
import com.trading.nevcoin.notification.application.ports.TokenWatchlistCommandPort;
import com.trading.nevcoin.notification.application.ports.WalletQueryPort;
import com.trading.nevcoin.notification.application.ports.WalletWatchlistCommandPort;
import com.trading.nevcoin.notification.domain.TelegramAccessPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    @Test
    void returnsLiveMarketDataForWatchedToken() {
        TokenQueryPort tokenQueryPort = new TokenQueryPort() {
            @Override public List<TokenSummary> list() { return List.of(); }
            @Override public TokenSummary find(String query) {
                return new TokenSummary("mint-address", null, null, "ACTIVE", Instant.parse("2026-09-30T00:00:00Z"));
            }
        };
        TokenMarketDataPort marketDataPort = mint -> Optional.of(new TokenMarketDataPort.TokenMarketData(
                mint, "MEME", "Meme Coin", new BigDecimal("0.00001234"),
                new BigDecimal("850000000"), new BigDecimal("12000000"),
                new BigDecimal("340000"), new BigDecimal("5.25"), 120, 84,
                Instant.parse("2026-09-30T00:05:00Z"), "DEX Screener / raydium"));
        TelegramCommandHandler marketHandler = new TelegramCommandHandler(
                new TelegramAccessPolicy(Set.of(ALLOWED_CHAT_ID)),
                () -> new SystemStatusPort.SystemStatus("UP", "paper-intelligence"),
                tokenQueryPort,
                marketDataPort,
                noOpTokenCommands(),
                emptyWalletQueries(),
                noOpWalletCommands());

        String response = marketHandler.handle(ALLOWED_CHAT_ID, "/token mint-address").orElseThrow();

        assertTrue(response.contains("Token: MEME"));
        assertTrue(response.contains("Price: $0.00001234"));
        assertTrue(response.contains("Market cap: $850,000,000.00"));
        assertTrue(response.contains("Buys/Sells 5m: 120 / 84"));
        assertTrue(response.contains("Source: DEX Screener / raydium"));
    }

    @Test
    void resolvesAndPersistsLiveSymbolForLegacyWatchlistEntry() {
        TokenQueryPort tokenQueryPort = new TokenQueryPort() {
            @Override public List<TokenSummary> list() {
                return List.of(new TokenSummary(
                        "oreoU2P8bN6jkk3jbaiVxYnG1dCXcYxwhwyK9jSybcp",
                        null, null, "ACTIVE", Instant.parse("2026-09-30T00:00:00Z")));
            }
            @Override public TokenSummary find(String query) { return null; }
        };
        TokenMarketDataPort marketDataPort = mint -> Optional.of(new TokenMarketDataPort.TokenMarketData(
                mint, "ORE", "ORE", new BigDecimal("143.18"),
                new BigDecimal("71445292"), new BigDecimal("565525.88"),
                new BigDecimal("416542.50"), new BigDecimal("65.32"), 26, 43,
                Instant.parse("2026-10-01T03:17:44Z"), "DEX Screener / meteora"));
        RecordingTokenCommands tokenCommands = new RecordingTokenCommands();
        TelegramCommandHandler marketHandler = new TelegramCommandHandler(
                new TelegramAccessPolicy(Set.of(ALLOWED_CHAT_ID)),
                () -> new SystemStatusPort.SystemStatus("UP", "paper-intelligence"),
                tokenQueryPort,
                marketDataPort,
                tokenCommands,
                emptyWalletQueries(),
                noOpWalletCommands());

        String response = marketHandler.handle(ALLOWED_CHAT_ID, "/token ORE").orElseThrow();

        assertTrue(response.contains("Token: ORE"));
        assertEquals("ORE", tokenCommands.updatedSymbol);
        assertEquals("ORE", tokenCommands.updatedName);
    }

    private TokenWatchlistCommandPort noOpTokenCommands() {
        return new TokenWatchlistCommandPort() {
            @Override public void watch(String mintAddress) { }
            @Override public void updateMetadata(String mintAddress, String symbol, String name) { }
            @Override public void unwatch(String mintAddress) { }
        };
    }

    private WalletQueryPort emptyWalletQueries() {
        return new WalletQueryPort() {
            @Override public List<WalletSummary> list() { return List.of(); }
            @Override public WalletSummary find(String address) { return null; }
        };
    }

    private WalletWatchlistCommandPort noOpWalletCommands() {
        return new WalletWatchlistCommandPort() {
            @Override public void watch(String address) { }
            @Override public void unwatch(String address) { }
        };
    }

    private static class RecordingTokenCommands implements TokenWatchlistCommandPort {
        private String updatedSymbol;
        private String updatedName;

        @Override public void watch(String mintAddress) { }
        @Override public void unwatch(String mintAddress) { }
        @Override public void updateMetadata(String mintAddress, String symbol, String name) {
            this.updatedSymbol = symbol;
            this.updatedName = name;
        }
    }
}
