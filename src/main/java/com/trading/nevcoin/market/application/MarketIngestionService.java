package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.application.ports.MarketDataProvider;
import com.trading.nevcoin.market.application.ports.MarketDataStore;
import com.trading.nevcoin.market.application.ports.MarketTradeStreamProvider;
import com.trading.nevcoin.market.domain.MarketSignal;
import com.trading.nevcoin.market.domain.MarketSnapshot;
import com.trading.nevcoin.market.domain.MarketTick;
import com.trading.nevcoin.market.domain.MarketTrade;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "market", name = "enabled", havingValue = "true")
public class MarketIngestionService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MarketIngestionService.class);

    private final MarketProperties properties;
    private final ObjectProvider<MarketDataProvider> providerProvider;
    private final MarketDataStore store;
    private final MarketSnapshotCalculator snapshotCalculator;
    private final MarketSignalDetector signalDetector;
    private final NotificationPort notificationPort;
    private final MarketTradeStreamProvider tradeStreamProvider;
    private final TradeWindowAggregator tradeWindowAggregator;
    private volatile boolean running;

    public MarketIngestionService(
            MarketProperties properties,
            ObjectProvider<MarketDataProvider> providerProvider,
            MarketDataStore store,
            MarketSnapshotCalculator snapshotCalculator,
            MarketSignalDetector signalDetector,
            NotificationPort notificationPort,
            MarketTradeStreamProvider tradeStreamProvider,
            TradeWindowAggregator tradeWindowAggregator) {
        this.properties = properties;
        this.providerProvider = providerProvider;
        this.store = store;
        this.snapshotCalculator = snapshotCalculator;
        this.signalDetector = signalDetector;
        this.notificationPort = notificationPort;
        this.tradeStreamProvider = tradeStreamProvider;
        this.tradeWindowAggregator = tradeWindowAggregator;
    }

    @Scheduled(fixedDelayString = "${market.poll-interval-ms:30000}")
    public void pollConfiguredTokens() {
        if (!properties.isEnabled() || properties.isStreamEnabled()) {
            return;
        }
        for (String tokenAddress : properties.parsedWatchMints()) {
            try {
                ingest(tokenAddress);
            } catch (RuntimeException exception) {
                log.warn("Market ingestion failed for tokenAddress={}", tokenAddress, exception);
            }
        }
    }

    @Transactional
    public void ingest(String tokenAddress) {
        MarketDataProvider provider = providerProvider.getIfAvailable();
        if (provider == null) {
            throw new IllegalStateException("No polling market data provider is configured");
        }
        MarketTick current = provider.fetch(tokenAddress);
        List<MarketTick> recentTicks = store.recentTicks(tokenAddress, 32);
        MarketTick previous = recentTicks.isEmpty() ? null : recentTicks.getFirst();
        MarketSnapshot snapshot = snapshotCalculator.calculate(current, recentTicks);

        store.saveTick(current);
        store.saveSnapshot(snapshot);

        processSignals(current, snapshot, previous);
    }

    private void ingestTrade(MarketTrade trade) {
        TradeWindowAggregator.TradeMetrics metrics = tradeWindowAggregator.add(trade);
        MarketTick current = new MarketTick(
                trade.tokenAddress(),
                trade.observedAt(),
                trade.sourceTimestamp(),
                trade.priceUsd(),
                null,
                null,
                null,
                trade.sourceQuality(),
                metrics.buyVolume5m(),
                metrics.sellVolume5m(),
                metrics.netFlow5m(),
                metrics.uniqueBuyers5m(),
                metrics.uniqueSellers5m());
        List<MarketTick> recentTicks = store.recentTicks(trade.tokenAddress(), 32);
        MarketTick previous = recentTicks.isEmpty() ? null : recentTicks.getFirst();
        MarketSnapshot snapshot = snapshotCalculator.calculate(current, recentTicks);
        store.saveTick(current);
        store.saveSnapshot(snapshot);
        processSignals(current, snapshot, previous);
    }

    private void processSignals(MarketTick current, MarketSnapshot snapshot, MarketTick previous) {
        for (MarketSignal signal : signalDetector.detect(current, snapshot, previous, properties)) {
            store.saveSignal(signal);
            publishAlert(signal, snapshot);
        }
    }

    private void publishAlert(MarketSignal signal, MarketSnapshot snapshot) {
        if (!properties.isAlertsEnabled()) {
            return;
        }
        String text = "⚠️ MARKET SIGNAL\n\n"
                + "Token: " + signal.tokenAddress() + "\n"
                + "Type: " + signal.type() + "\n"
                + "Strength: " + signal.strength() + "\n"
                + "Price: " + snapshot.priceUsd() + " USD\n"
                + "Liquidity: " + snapshot.liquidityUsd() + " USD\n"
                + "Observed: " + signal.observedAt() + "\n"
                + "Evidence: " + String.join("; ", signal.evidence()) + "\n\n"
                + "Descriptive intelligence only. No trade was executed.";

        for (Long chatId : properties.parsedAlertChatIds()) {
            notificationPort.send(new NotificationPort.Notification(chatId, text));
        }
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || !properties.isStreamEnabled() || running) {
            return;
        }
        tradeStreamProvider.start(properties.parsedWatchMints(), this::handleTrade);
        running = true;
        log.info("Market trade stream started");
    }

    @Override
    public void stop() {
        if (running) {
            tradeStreamProvider.stop();
            running = false;
            log.info("Market trade stream stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void handleTrade(MarketTrade trade) {
        try {
            ingestTrade(trade);
        } catch (RuntimeException exception) {
            log.warn("Market trade ingestion failed for tokenAddress={}", trade.tokenAddress(), exception);
        }
    }
}
