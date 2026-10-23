package com.trading.nevcoin.social.application;

import com.trading.nevcoin.market.application.ports.MarketTokenMetadataPort;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import com.trading.nevcoin.social.application.ports.SocialEventProvider;
import com.trading.nevcoin.social.application.ports.SocialEntityExtractor;
import com.trading.nevcoin.social.application.ports.SocialFetchResult;
import com.trading.nevcoin.social.application.ports.SocialCursorStore;
import com.trading.nevcoin.social.application.ports.SocialProcessingFailureStore;
import com.trading.nevcoin.social.application.ports.SocialRawEventStore;
import com.trading.nevcoin.social.application.ports.TokenAssociationResolver;
import com.trading.nevcoin.social.domain.RawSocialEvent;
import com.trading.nevcoin.social.domain.SocialConfidence;
import com.trading.nevcoin.social.domain.SocialEventStatus;
import com.trading.nevcoin.social.domain.SocialEventType;
import com.trading.nevcoin.social.domain.SocialEvidence;
import com.trading.nevcoin.social.application.ports.SocialEventStore;
import com.trading.nevcoin.social.domain.SocialEvent;
import com.trading.nevcoin.signal.application.SignalIntelligenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
    private final MarketTokenMetadataPort metadata;
    private final NotificationPort notifications;
    private final SocialEntityExtractor entityExtractor;
    private final TokenAssociationResolver associationResolver;
    private final SocialSignalService socialSignalService;
    private final SignalIntelligenceService signalIntelligenceService;

    public SocialIngestionService(SocialProperties properties, SocialEventProvider provider, SocialEventStore store,
                                  SocialRawEventStore rawEventStore, SocialCursorStore cursorStore,
                                  SocialProcessingFailureStore failureStore,
                                  MarketTokenMetadataPort metadata, NotificationPort notifications,
                                  SocialEntityExtractor entityExtractor, TokenAssociationResolver associationResolver,
                                  SocialSignalService socialSignalService, SignalIntelligenceService signalIntelligenceService) {
        this.properties = properties;
        this.provider = provider;
        this.store = store;
        this.rawEventStore = rawEventStore;
        this.cursorStore = cursorStore;
        this.failureStore = failureStore;
        this.metadata = metadata;
        this.notifications = notifications;
        this.entityExtractor = entityExtractor;
        this.associationResolver = associationResolver;
        this.socialSignalService = socialSignalService;
        this.signalIntelligenceService = signalIntelligenceService;
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
                        .forEach(event -> {
                            alert(event);
                            Instant detectedAt = Instant.now();
                            socialSignalService.observe(event, detectedAt)
                                    .forEach(signal -> signalIntelligenceService.observe(signal, detectedAt));
                        });
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
        List<com.trading.nevcoin.social.domain.SocialEntity> entities = entityExtractor.extract(raw.rawText());
        List<SocialEvidence> evidence = new ArrayList<>();
        evidence.add(new SocialEvidence("RAW_TEXT", raw.rawText(), raw.externalEventId(), raw.receivedAt(), SocialConfidence.MEDIUM));
        evidence.addAll(entities.stream()
                .map(entity -> new SocialEvidence("ENTITY_" + entity.type(), entity.value(), raw.externalEventId(),
                        raw.receivedAt(), SocialConfidence.MEDIUM))
                .toList());
        return new SocialEvent(raw.provider(), raw.sourceId(), raw.sourceAccountId(), raw.externalEventId(), raw.canonicalUrl(),
                raw.publishedAt(), raw.receivedAt(), Instant.now(), null, SocialEventType.UNKNOWN,
                raw.rawText(), fingerprint, SocialEventStatus.NORMALIZED, "social-v2",
                evidence, entities, List.of(), null, SocialConfidence.MEDIUM);
    }

    private SocialEvent associateToken(SocialEvent event) {
        List<com.trading.nevcoin.social.domain.TokenAssociation> associations =
                associationResolver.resolve(event.text(), event.entities(), event.externalEventId());
        if (associations.isEmpty()) return event;
        return new SocialEvent(event.provider(), event.sourceId(), event.sourceAccountId(), event.externalEventId(), event.canonicalUrl(),
                event.publishedAt(), event.receivedAt(), event.processedAt(), event.alertedAt(), SocialEventType.TOKEN_MENTION,
                event.normalizedContent(), event.fingerprint(), SocialEventStatus.ASSOCIATED, event.algorithmVersion(),
                event.structuredEvidence(), event.entities(), associations, event.relevanceScore(), event.confidence());
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
