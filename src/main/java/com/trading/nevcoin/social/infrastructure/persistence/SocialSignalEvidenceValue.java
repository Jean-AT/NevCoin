package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.domain.SocialSignalEvidence;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class SocialSignalEvidenceValue {
    @Column(name = "source_event_id", nullable = false) private String sourceEventId;
    private String sourceAccountId;
    @Column(nullable = false) private String evidenceType;
    private String value;
    @Column(nullable = false) private Instant observedAt;
    protected SocialSignalEvidenceValue() { }
    SocialSignalEvidenceValue(SocialSignalEvidence value) { sourceEventId = value.sourceEventId(); sourceAccountId = value.sourceAccountId(); evidenceType = value.evidenceType(); this.value = value.value(); observedAt = value.observedAt(); }
    SocialSignalEvidence toDomain() { return new SocialSignalEvidence(sourceEventId, sourceAccountId, evidenceType, value, observedAt); }
}
