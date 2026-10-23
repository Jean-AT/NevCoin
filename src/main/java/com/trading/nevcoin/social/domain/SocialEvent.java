package com.trading.nevcoin.social.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Immutable normalized social observation; it contains no trading action. */
public record SocialEvent(
        String provider,
        String sourceId,
        String sourceAccountId,
        String externalEventId,
        String canonicalUrl,
        Instant publishedAt,
        Instant receivedAt,
        Instant processedAt,
        Instant alertedAt,
        SocialEventType type,
        String normalizedContent,
        String fingerprint,
        SocialEventStatus status,
        String algorithmVersion,
        List<SocialEvidence> structuredEvidence,
        List<SocialEntity> entities,
        List<TokenAssociation> tokenAssociations,
        BigDecimal relevanceScore,
        SocialConfidence confidence) {

    /** Compatibility constructor for the prototype storage/query API. */
    public SocialEvent(String source, String externalId, String tokenAddress, String author,
                       String text, Instant observedAt, List<String> legacyEvidence,
                       Instant receivedAt, BigDecimal confidence) {
        this(source, source, author, externalId, null, observedAt, receivedAt, receivedAt, null,
                SocialEventType.UNKNOWN, text, null, SocialEventStatus.PUBLISHED, "legacy-v1",
                legacyEvidence == null ? List.of() : legacyEvidence.stream()
                        .map(value -> new SocialEvidence("LEGACY", value, externalId, observedAt, null)).toList(),
                List.of(),
                tokenAddress == null ? List.of() : List.of(new TokenAssociation(tokenAddress, "LEGACY", null, "legacy-v1", List.of())),
                null, confidence == null ? null : confidence.compareTo(BigDecimal.ONE) >= 0 ? SocialConfidence.HIGH : SocialConfidence.MEDIUM);
    }

    public SocialEvent {
        type = type == null ? SocialEventType.UNKNOWN : type;
        status = status == null ? SocialEventStatus.RECEIVED : status;
        algorithmVersion = algorithmVersion == null ? "social-v1" : algorithmVersion;
        structuredEvidence = structuredEvidence == null ? List.of() : List.copyOf(structuredEvidence);
        entities = entities == null ? List.of() : List.copyOf(entities);
        tokenAssociations = tokenAssociations == null ? List.of() : List.copyOf(tokenAssociations);
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
        publishedAt = publishedAt == null ? receivedAt : publishedAt;
    }

    public String tokenAddress() {
        return tokenAssociations.isEmpty() ? null : tokenAssociations.getFirst().tokenAddress();
    }

    public String text() { return normalizedContent; }

    public String source() { return provider; }

    public String externalId() { return externalEventId; }

    /** Compatibility view retained for the existing Telegram query contract. */
    public List<String> evidence() {
        return structuredEvidence.stream().map(SocialEvidence::value).toList();
    }

    public List<SocialEvidence> structuredEvidence() { return structuredEvidence; }

    public List<String> legacyEvidence() { return evidence(); }

    public BigDecimal legacyConfidence() { return relevanceScore; }

    public String author() { return sourceAccountId; }

    public Instant observedAt() { return publishedAt; }
}
