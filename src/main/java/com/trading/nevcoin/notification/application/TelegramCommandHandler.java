package com.trading.nevcoin.notification.application;

import com.trading.nevcoin.notification.application.ports.SystemStatusPort;
import com.trading.nevcoin.notification.application.ports.TokenMarketDataPort;
import com.trading.nevcoin.notification.application.ports.TokenQueryPort;
import com.trading.nevcoin.notification.application.ports.TokenWatchlistCommandPort;
import com.trading.nevcoin.notification.application.ports.WalletQueryPort;
import com.trading.nevcoin.notification.application.ports.WalletWatchlistCommandPort;
import com.trading.nevcoin.notification.domain.TelegramAccessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
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
        });
    }

    @Autowired
    public TelegramCommandHandler(
            TelegramAccessPolicy accessPolicy,
            SystemStatusPort systemStatusPort,
            TokenQueryPort tokenQueryPort,
            TokenMarketDataPort tokenMarketDataPort,
            TokenWatchlistCommandPort tokenWatchlistCommandPort,
            WalletQueryPort walletQueryPort,
            WalletWatchlistCommandPort walletWatchlistCommandPort) {
        this.accessPolicy = accessPolicy;
        this.systemStatusPort = systemStatusPort;
        this.tokenQueryPort = tokenQueryPort;
        this.tokenMarketDataPort = tokenMarketDataPort;
        this.tokenWatchlistCommandPort = tokenWatchlistCommandPort;
        this.walletQueryPort = walletQueryPort;
        this.walletWatchlistCommandPort = walletWatchlistCommandPort;
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

    private String argument(String[] parts) { return parts.length < 2 ? "" : parts[1]; }
    private String value(String value) { return value == null ? "-" : value; }

    private String firstPresent(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
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
