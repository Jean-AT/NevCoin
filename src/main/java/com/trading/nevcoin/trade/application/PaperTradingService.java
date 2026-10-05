package com.trading.nevcoin.trade.application;

import com.trading.nevcoin.market.application.ports.MarketDataProvider;
import com.trading.nevcoin.market.application.ports.MarketTokenMetadataPort;
import com.trading.nevcoin.market.application.ports.MarketWatchlistPort;
import com.trading.nevcoin.market.domain.MarketTick;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import com.trading.nevcoin.notification.application.ports.PaperTradingPort;
import com.trading.nevcoin.trade.application.ports.PaperPortfolioStore;
import com.trading.nevcoin.trade.domain.PaperPosition;
import com.trading.nevcoin.trade.domain.PaperTrade;
import com.trading.nevcoin.trade.domain.TradeAction;
import com.trading.nevcoin.trade.domain.TradeDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaperTradingService implements PaperTradingPort {

    private static final Logger log = LoggerFactory.getLogger(PaperTradingService.class);

    private final PaperTradingProperties properties;
    private final ObjectProvider<MarketDataProvider> marketDataProvider;
    private final MarketWatchlistPort watchlist;
    private final MarketTokenMetadataPort tokenMetadata;
    private final DecisionEngine decisionEngine;
    private final PaperPortfolioStore portfolio;
    private final NotificationPort notificationPort;
    private final Map<String, Instant> lastHoldAlerts = new ConcurrentHashMap<>();

    public PaperTradingService(
            PaperTradingProperties properties,
            ObjectProvider<MarketDataProvider> marketDataProvider,
            MarketWatchlistPort watchlist,
            MarketTokenMetadataPort tokenMetadata,
            DecisionEngine decisionEngine,
            PaperPortfolioStore portfolio,
            NotificationPort notificationPort) {
        this.properties = properties;
        this.marketDataProvider = marketDataProvider;
        this.watchlist = watchlist;
        this.tokenMetadata = tokenMetadata;
        this.decisionEngine = decisionEngine;
        this.portfolio = portfolio;
        this.notificationPort = notificationPort;
    }

    @Override
    @Transactional
    public void setEnabled(boolean enabled) {
        portfolio.setEnabled(enabled, properties.getInitialCashUsd());
    }

    @Override
    @Transactional
    public void reset() {
        portfolio.reset(properties.getInitialCashUsd());
        lastHoldAlerts.clear();
    }

    @Override
    @Transactional
    public PaperStatus status() {
        return new PaperStatus(
                portfolio.enabled(properties.isEnabled(), properties.getInitialCashUsd()),
                portfolio.cashBalance(properties.getInitialCashUsd()),
                portfolio.positions(),
                portfolio.recentTrades(10));
    }

    @Scheduled(fixedDelayString = "${paper.poll-interval-ms:30000}")
    @Transactional
    public void evaluateWatchedTokens() {
        if (!portfolio.enabled(properties.isEnabled(), properties.getInitialCashUsd())) {
            return;
        }
        MarketDataProvider provider = marketDataProvider.getIfAvailable();
        if (provider == null) {
            log.warn("Paper trading is enabled but no market data provider is configured");
            return;
        }
        for (String tokenAddress : watchlist.activeMintAddresses()) {
            try {
                evaluateToken(provider, tokenAddress);
            } catch (RuntimeException exception) {
                log.warn("Paper trading evaluation failed tokenAddress={}", tokenAddress, exception);
            }
        }
    }

    private void evaluateToken(MarketDataProvider provider, String tokenAddress) {
        MarketTick tick = provider.fetch(tokenAddress);
        Optional<PaperPosition> position = portfolio.position(tokenAddress);
        TradeDecision decision = riskAdjustedDecision(tick, position);
        if (decision.action() == TradeAction.BUY) {
            if (position.isEmpty() && portfolio.positions().size() >= properties.getMaxOpenPositions()) {
                notifyRiskBlock(tick, "Maximum open positions reached");
                return;
            }
            executeBuy(tick, decision).ifPresent(trade -> publishDecision(tick, decision, trade));
        } else if (decision.action() == TradeAction.SELL && position.isPresent()) {
            if (!isForcedExit(decision) && dailyLossLimitReached(tick, position.get())) {
                notifyRiskBlock(tick, "Maximum daily loss reached");
                return;
            }
            executeSell(tick, decision, position.get()).ifPresent(trade -> publishDecision(tick, decision, trade));
        } else {
            publishDecision(tick, decision, null);
        }
    }

    private TradeDecision riskAdjustedDecision(MarketTick tick, Optional<PaperPosition> position) {
        if (position.isPresent() && tick.priceUsd() != null && tick.priceUsd().signum() > 0) {
            BigDecimal entry = position.get().averageEntryPriceUsd();
            BigDecimal changePercent = tick.priceUsd().subtract(entry)
                    .divide(entry, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (properties.getStopLossPercent().signum() > 0
                    && changePercent.compareTo(properties.getStopLossPercent().negate()) <= 0) {
                return new TradeDecision(tick.tokenAddress(), TradeAction.SELL, BigDecimal.valueOf(100),
                        tick.observedAt(), List.of("Stop-loss reached at " + changePercent + "%"));
            }
            if (properties.getTakeProfitPercent().signum() > 0
                    && changePercent.compareTo(properties.getTakeProfitPercent()) >= 0) {
                return new TradeDecision(tick.tokenAddress(), TradeAction.SELL, BigDecimal.valueOf(100),
                        tick.observedAt(), List.of("Take-profit reached at " + changePercent + "%"));
            }
        }
        return decisionEngine.evaluate(tick, position);
    }

    private Optional<PaperTrade> executeBuy(MarketTick tick, TradeDecision decision) {
        BigDecimal cash = portfolio.cashBalance(properties.getInitialCashUsd());
        BigDecimal notional = properties.getTradeNotionalUsd()
                .min(properties.getMaxTradeNotionalUsd())
                .min(cash);
        if (tick.priceUsd() == null || tick.priceUsd().signum() <= 0 || notional.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal quantity = notional.divide(tick.priceUsd(), 18, RoundingMode.DOWN);
        if (quantity.signum() <= 0) {
            return Optional.empty();
        }
        PaperTrade trade = new PaperTrade(
                tick.tokenAddress(), TradeAction.BUY, quantity, tick.priceUsd(),
                notional, String.join("; ", decision.evidence()), Instant.now());
        portfolio.buy(trade);
        log.info("Paper BUY tokenAddress={} notionalUsd={} priceUsd={}",
                tick.tokenAddress(), notional, tick.priceUsd());
        return Optional.of(trade);
    }

    private Optional<PaperTrade> executeSell(MarketTick tick, TradeDecision decision, PaperPosition position) {
        if (tick.priceUsd() == null || tick.priceUsd().signum() <= 0 || position.quantity().signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal notional = position.quantity().multiply(tick.priceUsd());
        PaperTrade trade = new PaperTrade(
                tick.tokenAddress(), TradeAction.SELL, position.quantity(), tick.priceUsd(),
                notional, String.join("; ", decision.evidence()), Instant.now());
        portfolio.sell(trade);
        log.info("Paper SELL tokenAddress={} notionalUsd={} priceUsd={}",
                tick.tokenAddress(), notional, tick.priceUsd());
        return Optional.of(trade);
    }

    private boolean dailyLossLimitReached(MarketTick tick, PaperPosition position) {
        if (properties.getMaxDailyLossUsd().signum() <= 0) return false;
        BigDecimal unrealizedPnl = tick.priceUsd().subtract(position.averageEntryPriceUsd())
                .multiply(position.quantity());
        BigDecimal realizedPnl = portfolio.realizedPnlSince(Instant.now().truncatedTo(ChronoUnit.DAYS));
        return realizedPnl.add(unrealizedPnl).compareTo(properties.getMaxDailyLossUsd().negate()) <= 0;
    }

    private boolean isForcedExit(TradeDecision decision) {
        return decision.evidence().stream().anyMatch(evidence ->
                evidence.startsWith("Stop-loss") || evidence.startsWith("Take-profit"));
    }

    private void publishDecision(MarketTick tick, TradeDecision decision, PaperTrade trade) {
        if (!properties.isAlertsEnabled()
                || (decision.action() == TradeAction.HOLD && !properties.isNotifyHoldDecisions())) {
            return;
        }
        Instant now = Instant.now();
        if (decision.action() == TradeAction.HOLD) {
            Instant previous = lastHoldAlerts.get(tick.tokenAddress());
            if (previous != null && previous.plusMillis(properties.getHoldAlertIntervalMs()).isAfter(now)) {
                return;
            }
            lastHoldAlerts.put(tick.tokenAddress(), now);
        }
        String tokenName = tokenMetadata.findByMintAddress(tick.tokenAddress())
                .map(MarketTokenMetadataPort.TokenMetadata::displayName)
                .orElse(tick.tokenAddress());
        StringBuilder text = new StringBuilder("📊 PAPER DECISION\n\n")
                .append("Token: ").append(tokenName).append("\n")
                .append("Action: ").append(decision.action()).append("\n")
                .append("Price: $").append(tick.priceUsd()).append("\n")
                .append("Confidence: ").append(decision.confidence()).append("\n")
                .append("Reason: ").append(String.join("; ", decision.evidence())).append("\n");
        if (trade != null) {
            text.append("Notional: $").append(trade.notionalUsd()).append("\n");
            if (trade.realizedPnlUsd() != null) {
                text.append("Realized PnL: $").append(trade.realizedPnlUsd()).append("\n");
            }
        }
        text.append("Simulated only. No blockchain transaction was executed.");
        properties.parsedAlertChatIds().forEach(chatId -> sendSafely(chatId, text.toString()));
    }

    private void notifyRiskBlock(MarketTick tick, String reason) {
        if (!properties.isAlertsEnabled()) return;
        String tokenName = tokenMetadata.findByMintAddress(tick.tokenAddress())
                .map(MarketTokenMetadataPort.TokenMetadata::displayName)
                .orElse(tick.tokenAddress());
        String text = "🛡 PAPER RISK BLOCK\n\nToken: " + tokenName + "\nReason: " + reason
                + "\nSimulated only. No blockchain transaction was executed.";
        properties.parsedAlertChatIds().forEach(chatId -> sendSafely(chatId, text));
    }

    private void sendSafely(long chatId, String text) {
        try {
            notificationPort.send(new NotificationPort.Notification(chatId, text));
        } catch (RuntimeException exception) {
            log.warn("Paper trading notification failed chatId={}", chatId, exception);
        }
    }
}
