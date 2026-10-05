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
import com.trading.nevcoin.social.application.ports.SocialEventQueryPort;
import com.trading.nevcoin.notification.domain.TelegramAccessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
            /alerts - show market alert status
            /alerts-on <mint|symbol|all> - enable market alerts
            /alerts-off <mint|symbol|all> - disable market alerts
            /signals - show active market signals
            /social - show recent social intelligence events
            /paper-status - show simulated portfolio
            /paper-on - enable simulated trading
            /paper-off - disable simulated trading
            /paper-reset - reset simulated cash, positions and trade history
            /wallets - list tracked wallets
            /wallet <address> - show a tracked wallet
            /watch-wallet <address> - track a wallet
            /unwatch-wallet <address> - stop tracking a wallet

            NevCoin is paper intelligence only. Trading commands are disabled.
            """;

    private final TelegramAccessPolicy accessPolicy;
    private final SystemStatusPort systemStatusPort;
    private final TokenQueryPort tokenQueryPort;
    private final TokenMarketDataPort tokenMarketDataPort;
    private final TokenWatchlistCommandPort tokenWatchlistCommandPort;
    private final WalletQueryPort walletQueryPort;
    private final WalletWatchlistCommandPort walletWatchlistCommandPort;
    private final MarketAlertSettingsPort marketAlertSettingsPort;
    private final PaperTradingPort paperTradingPort;
    private final MarketSignalQueryPort marketSignalQueryPort;
    private final SocialEventQueryPort socialEventQueryPort;

    public TelegramCommandHandler(TelegramAccessPolicy accessPolicy, SystemStatusPort systemStatusPort) {
        this(accessPolicy, systemStatusPort, new TokenQueryPort() {
            @Override public java.util.List<TokenQueryPort.TokenSummary> list() { return java.util.List.of(); }
            @Override public TokenQueryPort.TokenSummary find(String query) { return null; }
        }, mintAddress -> Optional.empty(), new TokenWatchlistCommandPort() {
            @Override public void watch(String mintAddress) { }
            @Override public void updateMetadata(String mintAddress, String symbol, String name) { }
            @Override public void unwatch(String mintAddress) { }
        }, new WalletQueryPort() {
            @Override public java.util.List<WalletQueryPort.WalletSummary> list() { return java.util.List.of(); }
            @Override public WalletQueryPort.WalletSummary find(String address) { return null; }
        }, new WalletWatchlistCommandPort() {
                @Override public void watch(String address) { }
                @Override public void unwatch(String address) { }
                }, defaultAlertSettings(), defaultPaperTrading(), defaultSignalQuery(), defaultSocialQuery());
    }

    public TelegramCommandHandler(
            TelegramAccessPolicy accessPolicy,
            SystemStatusPort systemStatusPort,
            TokenQueryPort tokenQueryPort,
            TokenMarketDataPort tokenMarketDataPort,
            TokenWatchlistCommandPort tokenWatchlistCommandPort,
            WalletQueryPort walletQueryPort,
            WalletWatchlistCommandPort walletWatchlistCommandPort) {
        this(accessPolicy, systemStatusPort, tokenQueryPort, tokenMarketDataPort,
                tokenWatchlistCommandPort, walletQueryPort, walletWatchlistCommandPort,
                defaultAlertSettings(), defaultPaperTrading(), defaultSignalQuery(), defaultSocialQuery());
    }

    public TelegramCommandHandler(
            TelegramAccessPolicy accessPolicy,
            SystemStatusPort systemStatusPort,
            TokenQueryPort tokenQueryPort,
            TokenMarketDataPort tokenMarketDataPort,
            TokenWatchlistCommandPort tokenWatchlistCommandPort,
            WalletQueryPort walletQueryPort,
            WalletWatchlistCommandPort walletWatchlistCommandPort,
            MarketAlertSettingsPort marketAlertSettingsPort) {
        this(accessPolicy, systemStatusPort, tokenQueryPort, tokenMarketDataPort,
                tokenWatchlistCommandPort, walletQueryPort, walletWatchlistCommandPort,
                marketAlertSettingsPort, defaultPaperTrading(), defaultSignalQuery(), defaultSocialQuery());
    }

    public TelegramCommandHandler(
            TelegramAccessPolicy accessPolicy,
            SystemStatusPort systemStatusPort,
            TokenQueryPort tokenQueryPort,
            TokenMarketDataPort tokenMarketDataPort,
            TokenWatchlistCommandPort tokenWatchlistCommandPort,
            WalletQueryPort walletQueryPort,
            WalletWatchlistCommandPort walletWatchlistCommandPort,
            MarketAlertSettingsPort marketAlertSettingsPort,
            PaperTradingPort paperTradingPort) {
        this(accessPolicy, systemStatusPort, tokenQueryPort, tokenMarketDataPort,
                tokenWatchlistCommandPort, walletQueryPort, walletWatchlistCommandPort,
                marketAlertSettingsPort, paperTradingPort, defaultSignalQuery(), defaultSocialQuery());
    }

    @Autowired
    public TelegramCommandHandler(
            TelegramAccessPolicy accessPolicy,
            SystemStatusPort systemStatusPort,
            TokenQueryPort tokenQueryPort,
            TokenMarketDataPort tokenMarketDataPort,
            TokenWatchlistCommandPort tokenWatchlistCommandPort,
            WalletQueryPort walletQueryPort,
            WalletWatchlistCommandPort walletWatchlistCommandPort,
            MarketAlertSettingsPort marketAlertSettingsPort,
            PaperTradingPort paperTradingPort,
            MarketSignalQueryPort marketSignalQueryPort,
            SocialEventQueryPort socialEventQueryPort) {
        this.accessPolicy = accessPolicy;
        this.systemStatusPort = systemStatusPort;
        this.tokenQueryPort = tokenQueryPort;
        this.tokenMarketDataPort = tokenMarketDataPort;
        this.tokenWatchlistCommandPort = tokenWatchlistCommandPort;
        this.walletQueryPort = walletQueryPort;
        this.walletWatchlistCommandPort = walletWatchlistCommandPort;
        this.marketAlertSettingsPort = marketAlertSettingsPort;
        this.paperTradingPort = paperTradingPort;
        this.marketSignalQueryPort = marketSignalQueryPort;
        this.socialEventQueryPort = socialEventQueryPort;
    }

    /** Test and embedding-friendly constructor retaining the pre-social API. */
    public TelegramCommandHandler(
            TelegramAccessPolicy accessPolicy,
            SystemStatusPort systemStatusPort,
            TokenQueryPort tokenQueryPort,
            TokenMarketDataPort tokenMarketDataPort,
            TokenWatchlistCommandPort tokenWatchlistCommandPort,
            WalletQueryPort walletQueryPort,
            WalletWatchlistCommandPort walletWatchlistCommandPort,
            MarketAlertSettingsPort marketAlertSettingsPort,
            PaperTradingPort paperTradingPort,
            MarketSignalQueryPort marketSignalQueryPort) {
        this(accessPolicy, systemStatusPort, tokenQueryPort, tokenMarketDataPort,
                tokenWatchlistCommandPort, walletQueryPort, walletWatchlistCommandPort,
                marketAlertSettingsPort, paperTradingPort, marketSignalQueryPort, defaultSocialQuery());
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
            case "/alerts" -> Optional.of(alertsMessage());
            case "/alerts-on" -> Optional.of(updateAlerts(argument(parts), true));
            case "/alerts-off" -> Optional.of(updateAlerts(argument(parts), false));
            case "/signals" -> Optional.of(signalsMessage());
            case "/social" -> Optional.of(socialMessage());
            case "/paper-status" -> Optional.of(paperStatusMessage());
            case "/paper-on" -> Optional.of(setPaperTrading(true));
            case "/paper-off" -> Optional.of(setPaperTrading(false));
            case "/paper-reset" -> Optional.of(resetPaperTrading());
            case "/wallets" -> Optional.of(walletsMessage());
            case "/wallet" -> Optional.of(walletMessage(argument(parts)));
            case "/watch-wallet" -> Optional.of(watchWallet(argument(parts)));
            case "/unwatch-wallet" -> Optional.of(unwatchWallet(argument(parts)));
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
        ResolvedToken resolved = resolveToken(query);
        if (resolved == null) return "Token is not in the watchlist.";

        var token = resolved.token();
        var marketData = resolved.marketData() == null
                ? tokenMarketDataPort.find(token.mintAddress()).orElse(null)
                : resolved.marketData();
        if (marketData == null) {
            return "Token\nMint: %s\nSymbol: %s\nName: %s\nStatus: %s\n\nMarket data is currently unavailable."
                    .formatted(token.mintAddress(), value(token.symbol()), value(token.name()), token.status());
        }
        tokenWatchlistCommandPort.updateMetadata(
                token.mintAddress(), marketData.symbol(), marketData.name());

        String symbol = firstPresent(marketData.symbol(), token.symbol());
        String name = firstPresent(marketData.name(), token.name());
        return """
                Token: %s
                Name: %s
                Mint: %s
                Price: %s
                Market cap: %s
                Liquidity: %s
                Volume 24h: %s
                Change 24h: %s
                Buys/Sells 5m: %d / %d
                Updated: %s
                Source: %s
                Status: %s

                Descriptive intelligence only. No trade was executed.
                """.formatted(
                value(symbol),
                value(name),
                token.mintAddress(),
                formatPrice(marketData.priceUsd()),
                formatUsd(marketData.marketCapUsd()),
                formatUsd(marketData.liquidityUsd()),
                formatUsd(marketData.volume24hUsd()),
                formatPercent(marketData.priceChange24hPercent()),
                marketData.buys5m(),
                marketData.sells5m(),
                marketData.observedAt() == null ? "-" : DateTimeFormatter.ISO_INSTANT.format(marketData.observedAt()),
                value(marketData.source()),
                token.status());
    }

    private String watchToken(String mint) {
        if (mint.isBlank()) return "Usage: /watch-token <mint>";
        try {
            tokenWatchlistCommandPort.watch(mint);
            tokenMarketDataPort.find(mint).ifPresent(marketData -> tokenWatchlistCommandPort.updateMetadata(
                    mint, marketData.symbol(), marketData.name()));
            return "Token added to watchlist: " + mint;
        }
        catch (IllegalArgumentException exception) { return "Invalid token mint: " + exception.getMessage(); }
    }

    private String unwatchToken(String mint) {
        if (mint.isBlank()) return "Usage: /unwatch-token <mint>";
        try { tokenWatchlistCommandPort.unwatch(mint); return "Token removed from watchlist: " + mint; }
        catch (IllegalArgumentException exception) { return "Invalid token mint: " + exception.getMessage(); }
    }

    private String signalsMessage() {
        var signals = marketSignalQueryPort.activeSignals(Instant.now());
        if (signals.isEmpty()) return "Active market signals\nNone.";

        Map<String, java.util.List<MarketSignalQueryPort.SignalSummary>> grouped = signals.stream()
                .collect(Collectors.groupingBy(
                        MarketSignalQueryPort.SignalSummary::tokenAddress,
                        LinkedHashMap::new,
                        Collectors.toList()));
        StringBuilder message = new StringBuilder("Active market signals\n\n");
        grouped.forEach((mint, tokenSignals) -> {
            message.append("Token: ").append(signalTokenName(mint))
                    .append("\nSignals: ").append(tokenSignals.size()).append("\n");
            tokenSignals.forEach(signal -> {
                message.append("- ").append(signal.type())
                        .append(" | strength: ").append(signal.strength())
                        .append(" | observed: ").append(signal.observedAt()).append("\n")
                        .append("  Evidence: ").append(String.join("; ", signal.evidence())).append("\n");
                if (signal.priceUsd() != null || signal.liquidityUsd() != null
                        || signal.volume24hUsd() != null || signal.netFlow5m() != null) {
                    message.append("  Market: price=").append(formatPrice(signal.priceUsd()))
                            .append(", liquidity=").append(formatUsd(signal.liquidityUsd()))
                            .append(", volume24h=").append(formatUsd(signal.volume24hUsd()))
                            .append(", netFlow5m=").append(formatUsd(signal.netFlow5m())).append("\n");
                }
            });
            message.append("Paper engine: evaluated on its next scheduled cycle.\n\n");
        });
        return message.toString().trim();
    }

    private String socialMessage() {
        var events = socialEventQueryPort.recent(Instant.now().minusSeconds(24 * 60 * 60L));
        if (events.isEmpty()) return "Social intelligence (last 24h)\nNone.\n\nNo social provider is configured yet.";
        StringBuilder message = new StringBuilder("Social intelligence (last 24h)\n\n");
        events.forEach(event -> message.append("- ").append(event.source())
                .append(" | ").append(event.observedAt()).append("\n")
                .append("  Token: ").append(event.tokenAddress() == null ? "unassociated" : signalTokenName(event.tokenAddress())).append("\n")
                .append("  Author: ").append(value(event.author())).append("\n")
                .append("  ").append(value(event.text())).append("\n")
                .append("  Evidence: ").append(String.join("; ", event.evidence())).append("\n"));
        return message.toString().trim();
    }

    private String signalTokenName(String mintAddress) {
        TokenQueryPort.TokenSummary token = tokenQueryPort.find(mintAddress);
        return token == null ? mintAddress : tokenDisplayName(token);
    }

    private String alertsMessage() {
        var tokens = tokenQueryPort.list();
        String defaultStatus = marketAlertSettingsPort.defaultEnabled() ? "ON" : "OFF";
        if (tokens.isEmpty()) {
            return "Market alerts\nDefault: " + defaultStatus + "\nNo watched tokens.";
        }
        return "Market alerts\nDefault: " + defaultStatus + "\n" + tokens.stream()
                .map(token -> "- %s: %s".formatted(
                        tokenDisplayName(token),
                        marketAlertSettingsPort.isEnabledFor(token.mintAddress()) ? "ON" : "OFF"))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String updateAlerts(String query, boolean enabled) {
        String command = enabled ? "/alerts-on" : "/alerts-off";
        if (query.isBlank()) {
            return "Usage: " + command + " <mint|symbol|all>";
        }
        if ("all".equalsIgnoreCase(query)) {
            marketAlertSettingsPort.setAll(enabled);
            return "Market alerts " + (enabled ? "enabled" : "disabled") + " for all tokens.";
        }
        ResolvedToken resolved = resolveToken(query);
        if (resolved == null) {
            return "Token is not in the watchlist.";
        }
        marketAlertSettingsPort.setToken(resolved.token().mintAddress(), enabled);
        return "Market alerts " + (enabled ? "enabled" : "disabled") + " for "
                + tokenDisplayName(resolved.token()) + ".";
    }

    private String setPaperTrading(boolean enabled) {
        paperTradingPort.setEnabled(enabled);
        return "Paper trading " + (enabled ? "enabled" : "disabled")
                + ". No real blockchain transaction will be executed.";
    }

    private String resetPaperTrading() {
        paperTradingPort.reset();
        return "Paper trading portfolio reset. Cash and history were restored to the configured initial balance."
                + " No real blockchain transaction was executed.";
    }

    private String paperStatusMessage() {
        PaperTradingPort.PaperStatus status = paperTradingPort.status();
        StringBuilder message = new StringBuilder("Paper trading: ")
                .append(status.enabled() ? "ON" : "OFF")
                .append("\nCash: ").append(formatUsd(status.cashUsd()))
                .append("\nPositions:");
        if (status.positions().isEmpty()) {
            message.append(" none");
        } else {
            status.positions().forEach(position -> message.append("\n- ")
                    .append(position.tokenAddress())
                    .append(" | quantity: ").append(position.quantity())
                    .append(" | avg entry: ").append(formatPrice(position.averageEntryPriceUsd())));
        }
        if (!status.recentTrades().isEmpty()) {
            message.append("\nRecent simulated trades:");
            status.recentTrades().forEach(trade -> {
                message.append("\n- ")
                    .append(trade.action()).append(" ").append(trade.tokenAddress())
                    .append(" | ").append(formatUsd(trade.notionalUsd()))
                    .append(" @ ").append(formatPrice(trade.priceUsd()));
            if (trade.realizedPnlUsd() != null) {
                message.append(" | PnL: ").append(formatUsd(trade.realizedPnlUsd()));
            }
            });
        }
        return message.toString();
    }

    private String argument(String[] parts) { return parts.length < 2 ? "" : parts[1]; }
    private String value(String value) { return value == null ? "-" : value; }

    private String firstPresent(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private String tokenDisplayName(TokenQueryPort.TokenSummary token) {
        if (token.name() != null && !token.name().isBlank()
                && token.symbol() != null && !token.symbol().isBlank()
                && !token.name().equalsIgnoreCase(token.symbol())) {
            return token.name() + " (" + token.symbol() + ")";
        }
        if (token.name() != null && !token.name().isBlank()) return token.name();
        if (token.symbol() != null && !token.symbol().isBlank()) return token.symbol();
        return token.mintAddress();
    }

    private static MarketAlertSettingsPort defaultAlertSettings() {
        return new MarketAlertSettingsPort() {
            @Override public void setAll(boolean enabled) { }
            @Override public void setToken(String mintAddress, boolean enabled) { }
            @Override public boolean defaultEnabled() { return false; }
            @Override public boolean isEnabledFor(String mintAddress) { return false; }
        };
    }

    private static PaperTradingPort defaultPaperTrading() {
        return new PaperTradingPort() {
            @Override public void setEnabled(boolean enabled) { }
            @Override public void reset() { }
            @Override public PaperStatus status() {
                return new PaperStatus(false, BigDecimal.ZERO, java.util.List.of(), java.util.List.of());
            }
        };
    }

    private static SocialEventQueryPort defaultSocialQuery() {
        return since -> java.util.List.of();
    }

    private static MarketSignalQueryPort defaultSignalQuery() {
        return now -> java.util.List.of();
    }

    private ResolvedToken resolveToken(String query) {
        TokenQueryPort.TokenSummary stored = tokenQueryPort.find(query);
        if (stored != null) {
            return new ResolvedToken(stored, null);
        }

        for (TokenQueryPort.TokenSummary candidate : tokenQueryPort.list()) {
            var marketData = tokenMarketDataPort.find(candidate.mintAddress()).orElse(null);
            if (marketData != null && marketData.symbol() != null
                    && marketData.symbol().equalsIgnoreCase(query)) {
                tokenWatchlistCommandPort.updateMetadata(
                        candidate.mintAddress(), marketData.symbol(), marketData.name());
                return new ResolvedToken(candidate, marketData);
            }
        }
        return null;
    }

    private String formatPrice(BigDecimal value) {
        if (value == null) return "-";
        if (value.abs().compareTo(BigDecimal.ONE) < 0) {
            return "$" + value.setScale(12, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        }
        return formatUsd(value);
    }

    private String formatUsd(BigDecimal value) {
        if (value == null) return "-";
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return "$" + format.format(value);
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return "-";
        String prefix = value.signum() > 0 ? "+" : "";
        return prefix + value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private String walletsMessage() {
        var wallets = walletQueryPort.list();
        if (wallets.isEmpty()) return "No tracked wallets.";
        return "Tracked wallets\n" + wallets.stream().map(wallet -> "- %s [%s]".formatted(wallet.address(), wallet.status()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String walletMessage(String address) {
        if (address.isBlank()) return "Usage: /wallet <address>";
        var wallet = walletQueryPort.find(address);
        return wallet == null ? "Wallet is not tracked." : "Wallet\nAddress: %s\nStatus: %s".formatted(wallet.address(), wallet.status());
    }

    private String watchWallet(String address) {
        if (address.isBlank()) return "Usage: /watch-wallet <address>";
        try { walletWatchlistCommandPort.watch(address); return "Wallet added: " + address; }
        catch (IllegalArgumentException exception) { return "Invalid wallet: " + exception.getMessage(); }
    }

    private String unwatchWallet(String address) {
        if (address.isBlank()) return "Usage: /unwatch-wallet <address>";
        try { walletWatchlistCommandPort.unwatch(address); return "Wallet removed: " + address; }
        catch (IllegalArgumentException exception) { return "Invalid wallet: " + exception.getMessage(); }
    }

    private record ResolvedToken(
            TokenQueryPort.TokenSummary token,
            TokenMarketDataPort.TokenMarketData marketData) {
    }
}
