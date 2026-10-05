package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.application.ports.MarketDataProvider;
import com.trading.nevcoin.market.application.ports.MarketDataStore;
import com.trading.nevcoin.market.application.ports.MarketTradeStreamProvider;
import com.trading.nevcoin.market.application.ports.MarketWatchlistPort;
import com.trading.nevcoin.market.application.ports.MarketAlertPolicyPort;
import com.trading.nevcoin.market.application.ports.MarketTokenMetadataPort;
import com.trading.nevcoin.market.domain.MarketTick;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class MarketIngestionServiceTest {

    private static final String DATABASE_MINT = "oreoU2P8bN6jkk3jbaiVxYnG1dCXcYxwhwyK9jSybcp";
    private static final String STATIC_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v";

    @Test
    void startsAndSynchronizesStreamFromPersistedWatchlist() {
        MarketProperties properties = new MarketProperties();
        properties.setEnabled(true);
        properties.setStreamEnabled(true);
        properties.setWatchMints(STATIC_MINT);

        MarketWatchlistPort watchlist = mock(MarketWatchlistPort.class);
        when(watchlist.activeMintAddresses())
                .thenReturn(Set.of(DATABASE_MINT))
                .thenReturn(Set.of());
        MarketTradeStreamProvider stream = mock(MarketTradeStreamProvider.class);
        MarketIngestionService service = service(properties, watchlist, stream);

        service.start();
        verify(stream).start(eq(Set.of(STATIC_MINT, DATABASE_MINT)), any());

        service.syncStreamSubscriptions();
        verify(stream).updateSubscriptions(Set.of(STATIC_MINT));
    }

    @Test
    void startsStreamWithAnEmptyWatchlistSoTokensCanBeAddedLater() {
        MarketProperties properties = new MarketProperties();
        properties.setEnabled(true);
        properties.setStreamEnabled(true);

        MarketWatchlistPort watchlist = mock(MarketWatchlistPort.class);
        when(watchlist.activeMintAddresses()).thenReturn(Set.of());
        MarketTradeStreamProvider stream = mock(MarketTradeStreamProvider.class);
        MarketIngestionService service = service(properties, watchlist, stream);

        service.start();

        verify(stream).start(eq(Set.of()), any());
    }

    @Test
    void sendsNamedAlertOnceWhileSignalIsActive() {
        MarketProperties properties = new MarketProperties();
        properties.setEnabled(true);
        properties.setAlertChatIds("123");

        MarketDataProvider provider = mock(MarketDataProvider.class);
        Instant now = Instant.parse("2026-10-01T04:00:00Z");
        when(provider.fetch(DATABASE_MINT)).thenReturn(new MarketTick(
                DATABASE_MINT, now, now, BigDecimal.ONE, new BigDecimal("1000"),
                new BigDecimal("500"), new BigDecimal("8"), "DEX Screener / meteora"));
        ObjectProvider<MarketDataProvider> providerProvider = mock(ObjectProvider.class);
        when(providerProvider.getIfAvailable()).thenReturn(provider);

        MarketDataStore store = mock(MarketDataStore.class);
        when(store.recentTicks(DATABASE_MINT, 32)).thenReturn(List.of());
        when(store.hasActiveSignal(any(), any(), any())).thenReturn(false, true);
        MarketWatchlistPort watchlist = () -> Set.of(DATABASE_MINT);
        MarketAlertPolicyPort alertPolicy = ignored -> true;
        MarketTokenMetadataPort metadata = mint -> Optional.of(
                new MarketTokenMetadataPort.TokenMetadata(mint, "ORE", "ORE"));
        NotificationPort notifications = mock(NotificationPort.class);

        MarketIngestionService service = new MarketIngestionService(
                properties, providerProvider, store, new MarketSnapshotCalculator(),
                new MarketSignalDetector(), notifications, mock(MarketTradeStreamProvider.class),
                watchlist, alertPolicy, metadata, new TradeWindowAggregator());

        service.pollConfiguredTokens();
        service.pollConfiguredTokens();

        var captor = org.mockito.ArgumentCaptor.forClass(NotificationPort.Notification.class);
        verify(notifications, times(1)).send(captor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().text().contains("Token: ORE"));
        org.junit.jupiter.api.Assertions.assertFalse(captor.getValue().text().contains(DATABASE_MINT));
    }

    @SuppressWarnings("unchecked")
    private MarketIngestionService service(
            MarketProperties properties,
            MarketWatchlistPort watchlist,
            MarketTradeStreamProvider stream) {
        return new MarketIngestionService(
                properties,
                mock(ObjectProvider.class),
                mock(MarketDataStore.class),
                new MarketSnapshotCalculator(),
                new MarketSignalDetector(),
                mock(NotificationPort.class),
                stream,
                watchlist,
                ignored -> false,
                mint -> Optional.empty(),
                new TradeWindowAggregator());
    }
}
