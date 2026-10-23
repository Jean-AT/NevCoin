package com.trading.nevcoin.signal.infrastructure.persistence;

import com.trading.nevcoin.signal.domain.CrossDomainSignal;
import com.trading.nevcoin.signal.domain.SignalComponent;
import com.trading.nevcoin.signal.domain.SignalEvidence;
import com.trading.nevcoin.signal.domain.SignalPriority;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "cross_domain_signals")
public class CrossDomainSignalEntity {
    @Id private String id;
    @Column(nullable = false) private String tokenAddress;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SignalPriority priority;
    @Column(nullable = false, precision = 20, scale = 8) private BigDecimal strength;
    @Column(nullable = false, precision = 20, scale = 8) private BigDecimal confidence;
    @Column(nullable = false) private Instant detectedAt;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private String algorithmVersion;
    @ElementCollection(targetClass = SignalComponent.class) @Enumerated(EnumType.STRING)
    @CollectionTable(name = "cross_domain_signal_components", joinColumns = @JoinColumn(name = "signal_id"))
    @Column(name = "component", nullable = false) private Set<SignalComponent> components = Set.of();
    @ElementCollection @CollectionTable(name = "cross_domain_signal_evidence", joinColumns = @JoinColumn(name = "signal_id"))
    private List<SignalEvidenceValue> evidence = new ArrayList<>();
    protected CrossDomainSignalEntity() { }
    CrossDomainSignalEntity(CrossDomainSignal signal) {
        id = signal.signalId(); tokenAddress = signal.tokenAddress(); priority = signal.priority(); strength = signal.strength(); confidence = signal.confidence();
        detectedAt = signal.detectedAt(); expiresAt = signal.expiresAt(); algorithmVersion = signal.algorithmVersion(); components = signal.components();
        evidence = signal.evidence().stream().map(SignalEvidenceValue::new).toList();
    }
    CrossDomainSignal toDomain() { return new CrossDomainSignal(id, tokenAddress, priority, strength, confidence, detectedAt, expiresAt, components,
            evidence.stream().map(SignalEvidenceValue::toDomain).toList(), algorithmVersion); }
}
