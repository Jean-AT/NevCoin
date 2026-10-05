package com.trading.nevcoin.notification.application;

import com.trading.nevcoin.notification.application.ports.SystemStatusPort;
import com.trading.nevcoin.notification.application.ports.TokenMarketDataPort;
import com.trading.nevcoin.notification.application.ports.TokenQueryPort;
import com.trading.nevcoin.notification.application.ports.TokenWatchlistCommandPort;
import com.trading.nevcoin.notification.application.ports.WalletQueryPort;
import com.trading.nevcoin.notification.application.ports.WalletWatchlistCommandPort;
import com.trading.nevcoin.notification.application.ports.MarketAlertSettingsPort;
import com.trading.nevcoin.notification.application.ports.PaperTradingPort;
import com.trading.nevcoin.market.application.ports.MarketSignalQueryPort;
import com.trading.nevcoin.market.domain.MarketSignal;
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

    @Test
    void enablesAndDisablesAlertsBySymbolOrForAllTokens() {
        TokenQueryPort tokenQueryPort = new TokenQueryPort() {
            private final TokenSummary ore = new TokenSummary(
                    "oreoU2P8bN6jkk3jbaiVxYnG1dCXcYxwhwyK9jSybcp",
                    "ORE", "ORE", "ACTIVE", Instant.parse("2026-09-30T00:00:00Z"));
            @Override public List<TokenSummary> list() { return List.of(ore); }
            @Override public TokenSummary find(String query) {
                return "ORE".equalsIgnoreCase(query) ? ore : null;
            }
        };
        RecordingAlertSettings alertSettings = new RecordingAlertSettings();
        TelegramCommandHandler alertHandler = new TelegramCommandHandler(
                new TelegramAccessPolicy(Set.of(ALLOWED_CHAT_ID)),
                () -> new SystemStatusPort.SystemStatus("UP", "paper-intelligence"),
                tokenQueryPort,
                mint -> Optional.empty(),
                noOpTokenCommands(),
                emptyWalletQueries(),
                noOpWalletCommands(),
                alertSettings);

        assertEquals("Market alerts disabled for ORE.",
                alertHandler.handle(ALLOWED_CHAT_ID, "/alerts-off ORE").orElseThrow());
        assertEquals("oreoU2P8bN6jkk3jbaiVxYnG1dCXcYxwhwyK9jSybcp", alertSettings.tokenMint);
        assertEquals(false, alertSettings.tokenEnabled);

        assertEquals("Market alerts enabled for all tokens.",
                alertHandler.handle(ALLOWED_CHAT_ID, "/alerts-on all").orElseThrow());
        assertEquals(true, alertSettings.allEnabled);
    }

    @Test
    void togglesPaperTradingWithoutExposingRealTradingCommands() {
        RecordingPaperTrading paper = new RecordingPaperTrading();
        TelegramCommandHandler paperHandler = new TelegramCommandHandler(
                new TelegramAccessPolicy(Set.of(ALLOWED_CHAT_ID)),
                () -> new SystemStatusPort.SystemStatus("UP", "paper-intelligence"),
                emptyTokenQueries(), mint -> Optional.empty(), noOpTokenCommands(),
                emptyWalletQueries(), noOpWalletCommands(), new RecordingAlertSettings(), paper);

        assertTrue(paperHandler.handle(ALLOWED_CHAT_ID, "/paper-on").orElseThrow().contains("No real blockchain"));
        assertEquals(true, paper.enabled);
        assertTrue(paperHandler.handle(ALLOWED_CHAT_ID, "/paper-reset").orElseThrow().contains("portfolio reset"));
        assertEquals(true, paper.resetCalled);
        assertTrue(paperHandler.handle(ALLOWED_CHAT_ID, "/buy ORE").orElseThrow().contains("Unknown command"));
    }

    @Test
    void listsActiveSignalsGroupedByToken() {
        MarketSignalQueryPort signalQuery = now -> List.of(new MarketSignalQueryPort.SignalSummary(
                "mint-address", MarketSignal.SignalType.MOMENTUM_DETECTED, new BigDecimal("0.82"),
                Instant.parse("2026-10-04T00:00:00Z"), Instant.parse("2026-10-04T00:15:00Z"),
                List.of("24h momentum +8%"), new BigDecimal("1.20"),
                new BigDecimal("100000"), new BigDecimal("500000"), new BigDecimal("2500")));
        TokenQueryPort tokenQuery = new TokenQueryPort() {
            @Override public List<TokenSummary> list() { return List.of(); }
            @Override public TokenSummary find(String query) {
                return new TokenSummary("mint-address", "ORE", "Ore", "ACTIVE", Instant.now());
            }
        };
        TelegramCommandHandler signalsHandler = new TelegramCommandHandler(
                new TelegramAccessPolicy(Set.of(ALLOWED_CHAT_ID)),
                () -> new SystemStatusPort.SystemStatus("UP", "paper-intelligence"),
                tokenQuery, mint -> Optional.empty(), noOpTokenCommands(), emptyWalletQueries(),
                noOpWalletCommands(), new RecordingAlertSettings(), new RecordingPaperTrading(), signalQuery);

        String response = signalsHandler.handle(ALLOWED_CHAT_ID, "/signals").orElseThrow();

        assertTrue(response.contains("Token: Ore"));
        assertTrue(response.contains("MOMENTUM_DETECTED"));
        assertTrue(response.contains("24h momentum +8%"));
        assertTrue(response.contains("netFlow5m=$2,500.00"));
    }

    private TokenQueryPort emptyTokenQueries() {
        return new TokenQueryPort() {
            @Override public List<TokenSummary> list() { return List.of(); }
            @Override public TokenSummary find(String query) { return null; }
        };
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

    private static class RecordingAlertSettings implements MarketAlertSettingsPort {
        private String tokenMint;
        private boolean tokenEnabled;
        private boolean allEnabled;

        @Override public void setAll(boolean enabled) { this.allEnabled = enabled; }
        @Override public void setToken(String mintAddress, boolean enabled) {
            this.tokenMint = mintAddress;
            this.tokenEnabled = enabled;
        }
        @Override public boolean defaultEnabled() { return true; }
        @Override public boolean isEnabledFor(String mintAddress) { return true; }
    }

    private static class RecordingPaperTrading implements PaperTradingPort {
        private boolean enabled;
        private boolean resetCalled;
        @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
        @Override public void reset() { this.enabled = false; this.resetCalled = true; }
        @Override public PaperStatus status() {
            return new PaperStatus(enabled, BigDecimal.ZERO, List.of(), List.of());
        }
    }
}
