package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.domain.RawSocialEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Entity
@Table(name = "social_raw_events")
public class SocialRawEventEntity {
    @Id
    private String id;
    @Column(nullable = false, length = 64)
    private String provider;
    @Column(name = "source_id", nullable = false, length = 256)
    private String sourceId;
    @Column(name = "source_account_id", length = 256)
    private String sourceAccountId;
    @Column(name = "external_event_id", nullable = false, length = 512)
    private String externalEventId;
    @Column(name = "canonical_url", length = 1024)
    private String canonicalUrl;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;
    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    protected SocialRawEventEntity() { }

    public SocialRawEventEntity(RawSocialEvent event) {
        id = UUID.randomUUID().toString();
        provider = event.provider();
        sourceId = event.sourceId();
        sourceAccountId = event.sourceAccountId();
        externalEventId = event.externalEventId();
        canonicalUrl = event.canonicalUrl();
        publishedAt = event.publishedAt();
        receivedAt = event.receivedAt();
        rawText = event.rawText();
        payloadHash = hash(event.provider() + "|" + event.sourceId() + "|" + event.externalEventId() + "|" + event.rawText());
    }

    public RawSocialEvent toDomain() {
        return new RawSocialEvent(provider, sourceId, sourceAccountId, externalEventId, canonicalUrl,
                publishedAt, receivedAt, rawText, java.util.Map.of());
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to fingerprint raw social event", exception);
        }
    }
}
