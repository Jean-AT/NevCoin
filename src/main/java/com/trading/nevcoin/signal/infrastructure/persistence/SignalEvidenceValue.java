package com.trading.nevcoin.signal.infrastructure.persistence;

import com.trading.nevcoin.signal.domain.SignalComponent;
import com.trading.nevcoin.signal.domain.SignalEvidence;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

@Embeddable
public class SignalEvidenceValue {
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SignalComponent component;
    @Column(nullable = false) private String sourceId;
    @Column(nullable = false) private String description;
    @Column(nullable = false) private Instant observedAt;
    protected SignalEvidenceValue() { }
    SignalEvidenceValue(SignalEvidence value) { component = value.component(); sourceId = value.sourceId(); description = value.description(); observedAt = value.observedAt(); }
    SignalEvidence toDomain() { return new SignalEvidence(component, sourceId, description, observedAt); }
}
