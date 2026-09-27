package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.application.ports.MarketDataProvider;
import com.trading.nevcoin.market.application.ports.MarketDataStore;
import com.trading.nevcoin.market.domain.MarketSignal;
import com.trading.nevcoin.market.domain.MarketSnapshot;
import com.trading.nevcoin.market.domain.MarketTick;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MarketIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MarketIngestionService.class);

    private final MarketProperties properties;
    private final MarketDataProvider provider;
    private final MarketDataStore store;
    private final MarketSnapshotCalculator snapshotCalculator;
    private final MarketSignalDetector signalDetector;
    private final NotificationPort notificationPort;

    public MarketIngestionService(
            MarketProperties properties,
            MarketDataProvider provider,
            MarketDataStore store,
            MarketSnapshotCalculator snapshotCalculator,
            MarketSignalDetector signalDetector,
            NotificationPort notificationPort) {
        this.properties = properties;
        this.provider = provider;
        this.store = store;
        this.snapshotCalculator = snapshotCalculator;
        this.signalDetector = signalDetector;
        this.notificationPort = notificationPort;
    }

    @Scheduled(fixedDelayString = "${market.poll-interval-ms:30000}")
    public void pollConfiguredTokens() {
        if (!properties.isEnabled()) {
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
        MarketTick current = provider.fetch(tokenAddress);
        List<MarketTick> recentTicks = store.recentTicks(tokenAddress, 32);
        MarketTick previous = recentTicks.isEmpty() ? null : recentTicks.getFirst();
        MarketSnapshot snapshot = snapshotCalculator.calculate(current, recentTicks);

        store.saveTick(current);
        store.saveSnapshot(snapshot);

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
}
