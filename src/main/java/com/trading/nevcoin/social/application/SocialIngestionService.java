package com.trading.nevcoin.social.application;

import com.trading.nevcoin.market.application.ports.MarketTokenMetadataPort;
import com.trading.nevcoin.market.application.ports.MarketWatchlistPort;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import com.trading.nevcoin.social.application.ports.SocialEventProvider;
import com.trading.nevcoin.social.application.ports.SocialFetchResult;
import com.trading.nevcoin.social.application.ports.SocialCursorStore;
import com.trading.nevcoin.social.application.ports.SocialProcessingFailureStore;
import com.trading.nevcoin.social.application.ports.SocialRawEventStore;
import com.trading.nevcoin.social.domain.RawSocialEvent;
import com.trading.nevcoin.social.domain.SocialConfidence;
import com.trading.nevcoin.social.domain.SocialEventStatus;
import com.trading.nevcoin.social.domain.SocialEventType;
import com.trading.nevcoin.social.domain.SocialEvidence;
import com.trading.nevcoin.social.application.ports.SocialEventStore;
import com.trading.nevcoin.social.domain.SocialEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.time.Instant;

@Service
public class SocialIngestionService {
    private static final Logger log = LoggerFactory.getLogger(SocialIngestionService.class);
    private final SocialProperties properties;
    private final SocialEventProvider provider;
    private final SocialEventStore store;
    private final SocialRawEventStore rawEventStore;
    private final SocialCursorStore cursorStore;
    private final SocialProcessingFailureStore failureStore;
    private final MarketWatchlistPort watchlist;
    private final MarketTokenMetadataPort metadata;
    private final NotificationPort notifications;

    public SocialIngestionService(SocialProperties properties, SocialEventProvider provider, SocialEventStore store,
                                  SocialRawEventStore rawEventStore, SocialCursorStore cursorStore,
                                  SocialProcessingFailureStore failureStore, MarketWatchlistPort watchlist,
                                  MarketTokenMetadataPort metadata, NotificationPort notifications) {
        this.properties = properties;
        this.provider = provider;
        this.store = store;
        this.rawEventStore = rawEventStore;
        this.cursorStore = cursorStore;
        this.failureStore = failureStore;
        this.watchlist = watchlist;
        this.metadata = metadata;
        this.notifications = notifications;
    }

    @Scheduled(fixedDelayString = "${social.poll-interval-ms:120000}")
    public void poll() {
        if (!properties.isEnabled()) return;
        for (String url : urls()) {
            try {
                String sourceId = "rss:" + url;
                String cursor = cursorStore.current(sourceId).orElse(null);
                SocialFetchResult result = provider.fetch(sourceId, url, cursor, properties.getMaxEventsPerFeed());
                result.events().forEach(rawEventStore::saveIfNew);
                result.failures().forEach(failure -> failureStore.record(sourceId, failure.stage(), failure, Instant.now()));
                result.events().stream()
                        .map(this::normalize)
                        .map(this::associateToken)
                        .filter(event -> store.saveIfNew(event))
                        .forEach(this::alert);
                result.failures().forEach(failure -> log.warn("Social provider failure source={} category={} retryable={} message={}",
                        url, failure.category(), failure.retryable(), failure.message()));
                if (result.failures().stream().noneMatch(com.trading.nevcoin.social.application.ports.SocialProviderFailure::retryable)) {
                    cursorStore.advance(sourceId, result.nextCursor(), result.fetchedAt());
                }
            } catch (RuntimeException exception) {
                log.warn("Social ingestion failed url={}", url, exception);
            }
        }
    }

    private SocialEvent normalize(RawSocialEvent raw) {
        String fingerprint = UUID.nameUUIDFromBytes((raw.provider() + "|" + raw.sourceId() + "|"
                + raw.externalEventId() + "|" + raw.rawText()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        return new SocialEvent(raw.provider(), raw.sourceId(), raw.sourceAccountId(), raw.externalEventId(), raw.canonicalUrl(),
                raw.publishedAt(), raw.receivedAt(), Instant.now(), null, SocialEventType.UNKNOWN,
                raw.rawText(), fingerprint, SocialEventStatus.NORMALIZED, "social-v2",
                List.of(new SocialEvidence("RAW_TEXT", raw.rawText(), raw.externalEventId(), raw.receivedAt(), SocialConfidence.MEDIUM)),
                List.of(), null, SocialConfidence.MEDIUM);
    }

    private SocialEvent associateToken(SocialEvent event) {
        String haystack = (event.text() + " " + event.author()).toLowerCase(Locale.ROOT);
        for (String mint : watchlist.activeMintAddresses()) {
            var token = metadata.findByMintAddress(mint).orElse(null);
            if (haystack.contains(mint.toLowerCase(Locale.ROOT))
                    || (token != null && ((token.symbol() != null && haystack.contains(token.symbol().toLowerCase(Locale.ROOT)))
                    || (token.name() != null && haystack.contains(token.name().toLowerCase(Locale.ROOT))))) ) {
                return new SocialEvent(event.provider(), event.sourceId(), event.sourceAccountId(), event.externalEventId(), event.canonicalUrl(),
                        event.publishedAt(), event.receivedAt(), event.processedAt(), event.alertedAt(), SocialEventType.TOKEN_MENTION,
                        event.normalizedContent(), event.fingerprint(), SocialEventStatus.ASSOCIATED, event.algorithmVersion(),
                        event.structuredEvidence(), List.of(new com.trading.nevcoin.social.domain.TokenAssociation(mint, "DIRECT_NAME_REFERENCE", SocialConfidence.MEDIUM, "matcher-v1", event.structuredEvidence())),
                        event.relevanceScore(), event.confidence());
            }
        }
        return event;
    }

    private void alert(SocialEvent event) {
        if (!properties.isAlertsEnabled() || event.tokenAddress() == null) return;
        String text = "📣 SOCIAL EVENT\n\nToken: " + metadata.findByMintAddress(event.tokenAddress()).map(MarketTokenMetadataPort.TokenMetadata::displayName).orElse(event.tokenAddress())
                + "\nSource: " + event.source() + "\nObserved: " + event.observedAt() + "\n" + event.text()
                + "\n\nDescriptive intelligence only. No trade was executed.";
        properties.parsedAlertChatIds().forEach(chatId -> notifications.send(new NotificationPort.Notification(chatId, text)));
    }

    private List<String> urls() {
        if (properties.getRssUrls() == null || properties.getRssUrls().isBlank()) return List.of();
        List<String> values = new ArrayList<>();
        for (String value : properties.getRssUrls().split(",")) if (!value.isBlank()) values.add(value.trim());
        return values;
    }
}
