package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.domain.SocialEvent;
import com.trading.nevcoin.social.domain.SocialEventStatus;
import com.trading.nevcoin.social.domain.SocialEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "social_events")
public class SocialEventEntity {
    @Id
    private String id;
    @Column(nullable = false, length = 64)
    private String source;
    @Column(name = "provider", length = 64)
    private String provider;
    @Column(name = "source_id", length = 256)
    private String sourceId;
    @Column(name = "external_id", nullable = false, length = 512)
    private String externalId;
    @Column(name = "token_address", length = 100)
    private String tokenAddress;
    @Column(length = 256)
    private String author;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;
    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "alerted_at")
    private Instant alertedAt;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
    @Column(columnDefinition = "TEXT")
    private String evidence;
    @Column(precision = 8, scale = 6)
    private BigDecimal confidence;
    @Column(name = "event_type", length = 64)
    private String eventType;
    @Column(name = "normalized_content", columnDefinition = "TEXT")
    private String normalizedContent;
    @Column(length = 128)
    private String fingerprint;
    @Column(length = 32)
    private String status;
    @Column(name = "algorithm_version", length = 64)
    private String algorithmVersion;

    protected SocialEventEntity() { }

    public SocialEventEntity(SocialEvent event) {
        id = UUID.randomUUID().toString();
        source = event.source();
        provider = event.provider();
        sourceId = event.sourceId();
        externalId = event.externalEventId();
        tokenAddress = event.tokenAddress();
        author = event.author();
        text = event.text();
        observedAt = event.observedAt();
        publishedAt = event.publishedAt();
        processedAt = event.processedAt();
        alertedAt = event.alertedAt();
        receivedAt = event.receivedAt();
        evidence = String.join("\n", event.legacyEvidence());
        confidence = event.relevanceScore();
        eventType = event.type().name();
        normalizedContent = event.normalizedContent();
        fingerprint = event.fingerprint();
        status = event.status().name();
        algorithmVersion = event.algorithmVersion();
    }

    public SocialEvent toDomain() {
        List<String> values = evidence == null || evidence.isBlank() ? List.of() : Arrays.asList(evidence.split("\\n"));
        return new SocialEvent(provider == null ? "legacy" : provider,
                sourceId == null ? source : sourceId,
                author,
                externalId,
                null,
                publishedAt == null ? observedAt : publishedAt,
                receivedAt,
                processedAt,
                alertedAt,
                eventType == null ? SocialEventType.UNKNOWN : SocialEventType.valueOf(eventType),
                normalizedContent == null ? text : normalizedContent,
                fingerprint,
                status == null ? SocialEventStatus.PUBLISHED : SocialEventStatus.valueOf(status),
                algorithmVersion,
                values.stream().map(value -> new com.trading.nevcoin.social.domain.SocialEvidence("LEGACY", value, externalId, observedAt, null)).toList(),
                tokenAddress == null ? List.of() : List.of(new com.trading.nevcoin.social.domain.TokenAssociation(tokenAddress, "LEGACY", null, algorithmVersion, List.of())),
                confidence,
                confidence == null ? null : confidence.compareTo(BigDecimal.ONE) >= 0 ? com.trading.nevcoin.social.domain.SocialConfidence.HIGH : com.trading.nevcoin.social.domain.SocialConfidence.MEDIUM);
    }
}
