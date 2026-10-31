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
import com.trading.nevcoin.social.application.ports.SocialSignalStore;
import com.trading.nevcoin.social.domain.SocialSignal;
import com.trading.nevcoin.signal.application.SignalIntelligenceService;
import com.trading.nevcoin.signal.domain.CrossDomainSignal;
import com.trading.nevcoin.paper.validation.application.PaperValidationService;
import com.trading.nevcoin.paper.validation.domain.PaperValidationRequest;
import com.trading.nevcoin.paper.validation.domain.PaperValidationResult;
import com.trading.nevcoin.trade.domain.PaperTrade;
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
            🤖 NevCoin — ayuda rápida

            Empieza por /status para ver si todo está funcionando.

            📊 Consultas
            /status — resumen general del bot
            /health — comprobar si los servicios están bien
            /tokens — ver tus tokens vigilados
            /token <nombre o dirección> — ver precio y datos actuales
            /signals — ver señales del mercado
            /social — ver actividad social reciente
            /social-signals — ver señales sociales activas
            /cross-signals — ver señales combinadas
            /wallets — ver las wallets vigiladas
            /wallet <dirección> — ver una wallet concreta

            👀 Seguimiento
            /watch-token <dirección> — añadir un token
            /unwatch-token <dirección> — dejar de vigilar un token
            /watch-wallet <dirección> — añadir una wallet
            /unwatch-wallet <dirección> — dejar de vigilar una wallet

            🔔 Alertas
            /alerts — ver el estado de las alertas
            /alerts-on <nombre o all> — activar alertas
            /alerts-off <nombre o all> — desactivar alertas

            🧪 Simulación
            /paper-status — ver saldo y operaciones simuladas
            /paper-on — activar la simulación
            /paper-off — pausar la simulación
            /paper-reset — empezar la simulación desde cero
            /paper-validate [horas] — revisar cómo habría funcionado

            Ejemplos:
            /token ORE
            /watch-token <dirección-del-token>
            /alerts-on all

            ℹ️ NevCoin solo informa y simula. No compra ni vende con dinero real.
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
    private final SocialSignalStore socialSignalStore;
    private final SignalIntelligenceService signalIntelligenceService;
    private final PaperValidationService paperValidationService;

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
                }, defaultAlertSettings(), defaultPaperTrading(), defaultSignalQuery(), defaultSocialQuery(), null, null, null);
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
                defaultAlertSettings(), defaultPaperTrading(), defaultSignalQuery(), defaultSocialQuery(), null, null, null);
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
                marketAlertSettingsPort, defaultPaperTrading(), defaultSignalQuery(), defaultSocialQuery(), null, null, null);
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
                marketAlertSettingsPort, paperTradingPort, defaultSignalQuery(), defaultSocialQuery(), null, null, null);
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
            PaperTradingPort paperTradingPort,
            MarketSignalQueryPort marketSignalQueryPort,
            SocialEventQueryPort socialEventQueryPort) {
        this(accessPolicy, systemStatusPort, tokenQueryPort, tokenMarketDataPort, tokenWatchlistCommandPort,
                walletQueryPort, walletWatchlistCommandPort, marketAlertSettingsPort, paperTradingPort,
                marketSignalQueryPort, socialEventQueryPort, null, null, null);
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
            SocialEventQueryPort socialEventQueryPort,
            SocialSignalStore socialSignalStore,
            SignalIntelligenceService signalIntelligenceService,
            PaperValidationService paperValidationService) {
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
        this.socialSignalStore = socialSignalStore;
        this.signalIntelligenceService = signalIntelligenceService;
        this.paperValidationService = paperValidationService;
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
                marketAlertSettingsPort, paperTradingPort, marketSignalQueryPort, defaultSocialQuery(), null, null, null);
    }

    public Optional<String> handle(long chatId, String text) {
        if (!accessPolicy.isAllowed(chatId)) {
            log.warn("Ignoring Telegram command from unauthorized chatId={}", chatId);
            return Optional.empty();
        }

        String[] parts = text == null ? new String[0] : text.trim().split("\\s+");
        String command = normalizeCommand(parts.length == 0 ? "" : parts[0]);
        Optional<String> response = switch (command) {
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
            case "/social-signals" -> Optional.of(socialSignalsMessage());
            case "/cross-signals" -> Optional.of(crossSignalsMessage());
            case "/paper-validate" -> Optional.of(paperValidationMessage(argument(parts)));
            case "/paper-status" -> Optional.of(paperStatusMessage());
            case "/paper-on" -> Optional.of(setPaperTrading(true));
            case "/paper-off" -> Optional.of(setPaperTrading(false));
            case "/paper-reset" -> Optional.of(resetPaperTrading());
            case "/wallets" -> Optional.of(walletsMessage());
            case "/wallet" -> Optional.of(walletMessage(argument(parts)));
            case "/watch-wallet" -> Optional.of(watchWallet(argument(parts)));
            case "/unwatch-wallet" -> Optional.of(unwatchWallet(argument(parts)));
            case "" -> Optional.of("Escribe /help para ver los comandos disponibles.");
            default -> Optional.of("No reconocí ese comando: " + parts[0]
                    + "\n\nEscribe /help y te muestro qué puedes hacer.");
        };
        return response.map(this::fitTelegramMessage);
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
        return "📌 Estado de NevCoin\nModo: %s\nServicios: %s\nOperaciones reales: desactivadas\n\n"
                .formatted(friendlyMode(status.mode()), friendlyHealth(status.health()))
                + "Usa /tokens para ver tus tokens vigilados.";
    }

    private String healthMessage() {
        SystemStatusPort.SystemStatus status = systemStatusPort.current();
        return "🩺 Comprobación del bot\nServicios: %s\nOperaciones reales: desactivadas"
                .formatted(friendlyHealth(status.health()));
    }

    private String tokensMessage() {
        var tokens = tokenQueryPort.list();
        if (tokens.isEmpty()) return "👀 No tienes tokens vigilados todavía.\n\nAñade uno con:\n/watch-token <dirección-del-token>";
        return "👀 Tokens vigilados (" + tokens.size() + ")\n\n" + tokens.stream()
                .map(token -> "• %s\n  Dirección: %s\n  Estado: %s".formatted(
                        tokenDisplayName(token), token.mintAddress(), friendlyStatus(token.status())))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String tokenMessage(String query) {
        if (query.isBlank()) return "Para consultar un token escribe, por ejemplo:\n/token ORE\n\nTambién puedes usar su dirección completa.";
        ResolvedToken resolved = resolveToken(query);
        if (resolved == null) return "No encontré ese token entre los vigilados.\n\nAñádelo primero con:\n/watch-token <dirección-del-token>";

        var token = resolved.token();
        var marketData = resolved.marketData() == null
                ? tokenMarketDataPort.find(token.mintAddress()).orElse(null)
                : resolved.marketData();
        if (marketData == null) {
            return "🪙 Token\nNombre: %s\nSímbolo: %s\nDirección: %s\nEstado: %s\n\n"
                    .formatted(value(token.name()), value(token.symbol()), token.mintAddress(), friendlyStatus(token.status()))
                    + "Ahora mismo no pude obtener sus datos de mercado. Inténtalo de nuevo en unos segundos.";
        }
        tokenWatchlistCommandPort.updateMetadata(
                token.mintAddress(), marketData.symbol(), marketData.name());

        String symbol = firstPresent(marketData.symbol(), token.symbol());
        String name = firstPresent(marketData.name(), token.name());
        return """
                🪙 %s
                Nombre: %s
                Dirección: %s
                Precio: %s
                Capitalización: %s
                Liquidez: %s
                Volumen (24 h): %s
                Cambio (24 h): %s
                Compras/Ventas (5 min): %d / %d
                Actualizado: %s
                Fuente: %s
                Estado: %s

                ℹ️ Datos informativos. No se ejecutó ninguna operación.
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
                friendlyStatus(token.status()));
    }

    private String watchToken(String mint) {
        if (mint.isBlank()) return "Para añadir un token necesito su dirección.\nEjemplo: /watch-token ABC123...";
        try {
            tokenWatchlistCommandPort.watch(mint);
            tokenMarketDataPort.find(mint).ifPresent(marketData -> tokenWatchlistCommandPort.updateMetadata(
                    mint, marketData.symbol(), marketData.name()));
            return "✅ Token añadido a tu lista de vigilancia:\n" + mint
                    + "\n\nUsa /token " + mint + " para consultar sus datos.";
        }
        catch (IllegalArgumentException exception) { return "No pude añadir ese token. Revisa la dirección e inténtalo otra vez."; }
    }

    private String unwatchToken(String mint) {
        if (mint.isBlank()) return "Indica la dirección del token que quieres dejar de vigilar.\nEjemplo: /unwatch-token ABC123...";
        try { tokenWatchlistCommandPort.unwatch(mint); return "✅ Dejaste de vigilar este token:\n" + mint; }
        catch (IllegalArgumentException exception) { return "No pude quitar ese token. Revisa la dirección e inténtalo otra vez."; }
    }

    private String signalsMessage() {
        var signals = marketSignalQueryPort.activeSignals(Instant.now());
        if (signals.isEmpty()) return "📈 Señales del mercado\n\nNo hay señales activas en este momento.\nTe avisaré cuando aparezca algo relevante.";

        Map<String, java.util.List<MarketSignalQueryPort.SignalSummary>> grouped = signals.stream()
                .collect(Collectors.groupingBy(
                        MarketSignalQueryPort.SignalSummary::tokenAddress,
                        LinkedHashMap::new,
                        Collectors.toList()));
        StringBuilder message = new StringBuilder("📈 Señales del mercado\n\n");
        grouped.forEach((mint, tokenSignals) -> {
            message.append("🪙 ").append(signalTokenName(mint))
                    .append("\nSeñales detectadas: ").append(tokenSignals.size()).append("\n");
            tokenSignals.forEach(signal -> {
                message.append("• ").append(friendlySignalType(signal.type().name()))
                        .append(" | intensidad: ").append(formatPercent(signal.strength().multiply(new BigDecimal("100"))))
                        .append(" | detectada: ").append(signal.observedAt()).append("\n")
                        .append("  Datos: ").append(String.join("; ", signal.evidence())).append("\n");
                if (signal.priceUsd() != null || signal.liquidityUsd() != null
                        || signal.volume24hUsd() != null || signal.netFlow5m() != null) {
                    message.append("  Mercado: precio=").append(formatPrice(signal.priceUsd()))
                            .append(", liquidez=").append(formatUsd(signal.liquidityUsd()))
                            .append(", volumen 24 h=").append(formatUsd(signal.volume24hUsd()))
                            .append(", flujo neto 5 min=").append(formatUsd(signal.netFlow5m())).append("\n");
                }
            });
            message.append("La simulación volverá a evaluar este token en su próximo ciclo.\n\n");
        });
        return message.toString().trim();
    }

    private String socialMessage() {
        var events = socialEventQueryPort.recent(Instant.now().minusSeconds(24 * 60 * 60L));
        if (events.isEmpty()) return "📣 Actividad social (últimas 24 h)\n\nNo hay publicaciones relevantes todavía.\n\nSi esperabas datos, revisa la configuración de las fuentes sociales.";
        StringBuilder message = new StringBuilder("📣 Actividad social (últimas 24 h)\n\n");
        events.forEach(event -> message.append("• Fuente: ").append(event.source())
                .append(" | ").append(event.observedAt()).append("\n")
                .append("  Token relacionado: ").append(event.tokenAddress() == null ? "aún no identificado" : signalTokenName(event.tokenAddress())).append("\n")
                .append("  Autor: ").append(value(event.author())).append("\n")
                .append("  Publicación: ").append(value(event.text())).append("\n")
                .append("  Cómo se detectó: ").append(String.join("; ", event.evidence())).append("\n"));
        return message.toString().trim();
    }

    private String socialSignalsMessage() {
        if (socialSignalStore == null) {
            return "📣 Las señales sociales todavía no están configuradas en esta instalación.";
        }
        Instant now = Instant.now();
        var signals = socialSignalStore.recent(now.minusSeconds(24 * 60 * 60L)).stream()
                .filter(signal -> signal.isActiveAt(now)).toList();
        if (signals.isEmpty()) return "📣 Señales sociales activas (últimas 24 h)\n\nNo hay señales activas ahora mismo.";
        StringBuilder message = new StringBuilder("📣 Señales sociales activas (últimas 24 h)\n\n");
        signals.forEach(signal -> message.append("• ").append(signalTokenName(signal.tokenAddress()))
                .append("\n  Tipo: ").append(friendlySignalType(signal.signalType().name()))
                .append("\n  Confianza: ").append(formatPercent(signal.confidence().multiply(new BigDecimal("100"))))
                .append("\n  Detectada: ").append(signal.lastObservedAt()).append("\n")
                .append("  Evidencia: ").append(signal.evidence().stream()
                        .map(evidence -> evidence.evidenceType() + "=" + evidence.value())
                        .collect(Collectors.joining("; "))).append("\n"));
        return message.toString().trim();
    }

    private String crossSignalsMessage() {
        if (signalIntelligenceService == null) {
            return "🔎 Las señales combinadas todavía no están configuradas en esta instalación.";
        }
        var signals = signalIntelligenceService.highPriority(Instant.now());
        if (signals.isEmpty()) return "🔎 Señales combinadas\n\nTodavía no hay una señal confirmada por varias fuentes.";
        StringBuilder message = new StringBuilder("🔎 Señales combinadas\n\n");
        signals.forEach(signal -> message.append("• ").append(signalTokenName(signal.tokenAddress()))
                .append("\n  Prioridad: ").append(friendlyPriority(signal.priority().name()))
                .append("\n  Confianza: ").append(formatPercent(signal.confidence().multiply(new BigDecimal("100"))))
                .append("\n  Fuentes: ").append(signal.components())
                .append("\n  Detectada: ").append(signal.detectedAt()).append("\n")
                .append("  Evidencia: ").append(signal.evidence().stream()
                        .map(evidence -> evidence.component() + "=" + evidence.description())
                        .collect(Collectors.joining("; "))).append("\n"));
        message.append("\nℹ️ Esto es información combinada. No se ejecutó ninguna operación.");
        return message.toString();
    }

    private String paperValidationMessage(String hoursArgument) {
        if (paperValidationService == null) {
            return "🧪 La revisión de la simulación todavía no está configurada en esta instalación.";
        }
        int hours = 24;
        if (!hoursArgument.isBlank()) {
            try {
                hours = Integer.parseInt(hoursArgument);
                if (hours < 1 || hours > 168) return "Indica entre 1 y 168 horas. Ejemplo: /paper-validate 24";
            } catch (NumberFormatException exception) {
                return "No entendí ese número de horas. Ejemplo: /paper-validate 24";
            }
        }
        Instant to = Instant.now();
        PaperValidationRequest request = new PaperValidationRequest(
                to.minusSeconds(hours * 3600L), to, new BigDecimal("1000"), new BigDecimal("100"),
                BigDecimal.ZERO, 0, 5, java.util.Set.of(), java.util.Set.of());
        try {
            PaperValidationResult result = paperValidationService.validate(request);
            return "🧪 Revisión de la simulación (últimas " + hours + " h)\n"
                    + "Saldo inicial: " + formatUsd(result.initialCashUsd()) + "\n"
                    + "Saldo final: " + formatUsd(result.finalCashUsd()) + "\n"
                    + "Resultado: " + formatSignedUsd(result.totalReturnUsd()) + " ("
                    + formatPercent(result.totalReturnPercent()) + ")\n"
                    + "Operaciones simuladas: " + result.totalTrades() + "\n"
                    + "Operaciones con ganancia: " + formatPercent(result.winRate()) + "\n"
                    + "Mayor caída: " + formatUsd(result.maxDrawdownUsd()) + "\n\n"
                    + "ℹ️ Es una simulación informativa. No se ejecutó ninguna operación real.";
        } catch (IllegalStateException exception) {
            return "Todavía no puedo revisar el historial: la reproducción histórica no está configurada.";
        }
    }

    private String signalTokenName(String mintAddress) {
        TokenQueryPort.TokenSummary token = tokenQueryPort.find(mintAddress);
        return token == null ? mintAddress : tokenDisplayName(token);
    }

    private String alertsMessage() {
        var tokens = tokenQueryPort.list();
        String defaultStatus = marketAlertSettingsPort.defaultEnabled() ? "activadas" : "desactivadas";
        if (tokens.isEmpty()) {
            return "🔔 Alertas de mercado\nEstado general: " + defaultStatus
                    + "\n\nNo tienes tokens vigilados. Añade uno con /watch-token.";
        }
        return "🔔 Alertas de mercado\nEstado general: " + defaultStatus + "\n\n" + tokens.stream()
                .map(token -> "• %s: %s".formatted(
                        tokenDisplayName(token),
                        marketAlertSettingsPort.isEnabledFor(token.mintAddress()) ? "activadas" : "desactivadas"))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String updateAlerts(String query, boolean enabled) {
        String command = enabled ? "/alerts-on" : "/alerts-off";
        if (query.isBlank()) {
            return "Indica el token o escribe all.\nEjemplos:\n" + command + " ORE\n" + command + " all";
        }
        if ("all".equalsIgnoreCase(query)) {
            marketAlertSettingsPort.setAll(enabled);
            return "✅ Alertas " + (enabled ? "activadas" : "desactivadas") + " para todos tus tokens.";
        }
        ResolvedToken resolved = resolveToken(query);
        if (resolved == null) {
            return "No encontré ese token entre los vigilados. Usa /tokens para ver la lista.";
        }
        marketAlertSettingsPort.setToken(resolved.token().mintAddress(), enabled);
        return "✅ Alertas " + (enabled ? "activadas" : "desactivadas") + " para "
                + tokenDisplayName(resolved.token()) + ".";
    }

    private String setPaperTrading(boolean enabled) {
        paperTradingPort.setEnabled(enabled);
        return "🧪 Simulación " + (enabled ? "activada" : "pausada")
                + ".\nNo se enviará ninguna transacción ni se moverá dinero real.";
    }

    private String resetPaperTrading() {
        paperTradingPort.reset();
        return "♻️ Simulación reiniciada.\nEl saldo, las posiciones y el historial volvieron al estado inicial."
                + "\nNo se ejecutó ninguna operación real.";
    }

    private String paperStatusMessage() {
        PaperTradingPort.PaperStatus status = paperTradingPort.status();
        BigDecimal realizedPnl = status.recentTrades().stream()
                .map(PaperTrade::realizedPnlUsd)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        StringBuilder message = new StringBuilder("🧪 Estado de la simulación: ")
                .append(status.enabled() ? "ACTIVA" : "PAUSADA")
                .append("\nSaldo disponible: ").append(formatUsd(status.cashUsd()))
                .append("\nResultado acumulado: ").append(formatSignedUsd(realizedPnl))
                .append("\n\nPosiciones abiertas:");
        if (status.positions().isEmpty()) {
            message.append(" ninguna");
        } else {
            status.positions().forEach(position -> {
                TokenQueryPort.TokenSummary token = tokenQueryPort.find(position.tokenAddress());
                message.append("\n- ").append(token == null ? shortenMint(position.tokenAddress()) : tokenDisplayName(token))
                        .append("\n  Dirección: ").append(shortenMint(position.tokenAddress()))
                        .append("\n  Cantidad: ").append(formatQuantity(position.quantity()))
                        .append("\n  Precio medio: ").append(formatPrice(position.averageEntryPriceUsd()));
            });
        }
        if (!status.recentTrades().isEmpty()) {
            message.append("\n\nÚltimos movimientos simulados:");
            status.recentTrades().forEach(trade -> {
                TokenQueryPort.TokenSummary token = tokenQueryPort.find(trade.tokenAddress());
                message.append("\n• ").append(friendlyTradeAction(trade.action().name())).append(" ")
                    .append(token == null ? shortenMint(trade.tokenAddress()) : tokenDisplayName(token))
                    .append(" | importe: ").append(formatUsd(trade.notionalUsd()))
                    .append(" | cantidad: ").append(formatQuantity(trade.quantity()))
                    .append(" | precio: ").append(formatPrice(trade.priceUsd()));
            if (trade.realizedPnlUsd() != null) {
                message.append(" | resultado: ").append(formatSignedUsd(trade.realizedPnlUsd()));
            }
            });
        }
        return message.append("\n\nℹ️ Todo lo anterior es una simulación; no se ejecutaron compras ni ventas reales.").toString();
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
        int decimals = value.abs().compareTo(new BigDecimal("0.01")) < 0 ? 6 : 2;
        format.setMinimumFractionDigits(decimals);
        format.setMaximumFractionDigits(decimals);
        return "$" + format.format(value);
    }

    private String formatSignedUsd(BigDecimal value) {
        if (value == null) return "-";
        return (value.signum() > 0 ? "+" : "") + formatUsd(value);
    }

    private String formatQuantity(BigDecimal value) {
        if (value == null) return "-";
        return value.stripTrailingZeros().toPlainString();
    }

    private String shortenMint(String mint) {
        if (mint == null || mint.length() <= 14) return mint;
        return mint.substring(0, 6) + "…" + mint.substring(mint.length() - 6);
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return "-";
        String prefix = value.signum() > 0 ? "+" : "";
        return prefix + value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /** Telegram rejects messages over 4096 characters; keep command replies actionable. */
    private String fitTelegramMessage(String message) {
        if (message == null || message.length() <= 3800) return message;
        return message.substring(0, 3700)
                + "\n\n… La respuesta era muy larga para Telegram y fue recortada."
                + "\nUsa /token <símbolo> para consultar un token concreto.";
    }

    private String walletsMessage() {
        var wallets = walletQueryPort.list();
        if (wallets.isEmpty()) return "👛 No tienes wallets vigiladas todavía.\n\nAñade una con:\n/watch-wallet <dirección>";
        return "👛 Wallets vigiladas (" + wallets.size() + ")\n\n" + wallets.stream().map(wallet -> "• " + shortenMint(wallet.address())
                        + "\n  Dirección: " + wallet.address() + "\n  Estado: " + friendlyStatus(wallet.status()))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String walletMessage(String address) {
        if (address.isBlank()) return "Para consultar una wallet escribe:\n/wallet <dirección-de-la-wallet>";
        var wallet = walletQueryPort.find(address);
        return wallet == null
                ? "No encontré esa wallet entre las vigiladas.\n\nAñádela con:\n/watch-wallet " + address
                : "👛 Wallet\nDirección: %s\nEstado: %s".formatted(wallet.address(), friendlyStatus(wallet.status()));
    }

    private String watchWallet(String address) {
        if (address.isBlank()) return "Para vigilar una wallet necesito su dirección.\nEjemplo: /watch-wallet ABC123...";
        try { walletWatchlistCommandPort.watch(address); return "✅ Wallet añadida a tu lista de vigilancia:\n" + address; }
        catch (IllegalArgumentException exception) { return "No pude añadir esa wallet. Revisa la dirección e inténtalo otra vez."; }
    }

    private String unwatchWallet(String address) {
        if (address.isBlank()) return "Indica la dirección de la wallet que quieres dejar de vigilar.\nEjemplo: /unwatch-wallet ABC123...";
        try { walletWatchlistCommandPort.unwatch(address); return "✅ Dejaste de vigilar esta wallet:\n" + address; }
        catch (IllegalArgumentException exception) { return "No pude quitar esa wallet. Revisa la dirección e inténtalo otra vez."; }
    }

    private String friendlyStatus(String status) {
        if (status == null || status.isBlank()) return "sin información";
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "ACTIVE", "ENABLED", "UP" -> "activo";
            case "INACTIVE", "DISABLED", "DOWN" -> "inactivo";
            case "PAUSED" -> "pausado";
            default -> status.toLowerCase(Locale.ROOT);
        };
    }

    private String friendlyHealth(String health) {
        if (health == null || health.isBlank()) return "sin información";
        return switch (health.toUpperCase(Locale.ROOT)) {
            case "UP", "HEALTHY", "OK" -> "operativos ✅";
            case "DOWN", "UNHEALTHY" -> "con problemas ⚠️";
            default -> health.toLowerCase(Locale.ROOT);
        };
    }

    private String friendlyMode(String mode) {
        if (mode == null || mode.isBlank()) return "no especificado";
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "paper-intelligence", "paper" -> "inteligencia y simulación";
            default -> mode;
        };
    }

    private String friendlySignalType(String type) {
        if (type == null) return "señal de mercado";
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "MOMENTUM_DETECTED" -> "movimiento fuerte del precio";
            case "VOLUME_ACCELERATION" -> "volumen creciendo rápidamente";
            case "LIQUIDITY_CHANGE" -> "cambio importante de liquidez";
            case "WALLET_FLOW" -> "movimiento de wallets vigiladas";
            case "DIRECT_TOKEN_MENTION" -> "mención directa del token";
            case "MENTION_ACCELERATION" -> "aumento de menciones";
            case "MULTI_SOURCE_MENTION" -> "mención en varias fuentes";
            case "NARRATIVE_EMERGENCE" -> "narrativa en crecimiento";
            default -> type.toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    private String friendlyConfidence(String confidence) {
        if (confidence == null) return "sin calcular";
        return switch (confidence.toUpperCase(Locale.ROOT)) {
            case "VERY_HIGH" -> "muy alta";
            case "HIGH" -> "alta";
            case "MEDIUM" -> "media";
            case "LOW" -> "baja";
            case "VERY_LOW" -> "muy baja";
            default -> confidence.toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    private String friendlyPriority(String priority) {
        if (priority == null) return "normal";
        return switch (priority.toUpperCase(Locale.ROOT)) {
            case "VERY_HIGH", "CRITICAL" -> "muy alta";
            case "HIGH" -> "alta";
            case "LOW" -> "baja";
            default -> "normal";
        };
    }

    private String friendlyTradeAction(String action) {
        if (action == null) return "movimiento";
        return switch (action.toUpperCase(Locale.ROOT)) {
            case "BUY" -> "COMPRA simulada";
            case "SELL" -> "VENTA simulada";
            default -> action.toLowerCase(Locale.ROOT);
        };
    }

    private record ResolvedToken(
            TokenQueryPort.TokenSummary token,
            TokenMarketDataPort.TokenMarketData marketData) {
    }
}
