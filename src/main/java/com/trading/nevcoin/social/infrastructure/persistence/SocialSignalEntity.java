package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.domain.SocialSignal;
import com.trading.nevcoin.social.domain.SocialSignalEvidence;
import com.trading.nevcoin.social.domain.SocialSignalType;
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

@Entity
@Table(name = "social_signals")
public class SocialSignalEntity {
    @Id private String id;
    @Column(nullable = false) private String tokenAddress;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SocialSignalType signalType;
    @Column(nullable = false, precision = 20, scale = 8) private BigDecimal strength;
    @Column(nullable = false, precision = 20, scale = 8) private BigDecimal confidence;
    @Column(nullable = false) private Instant firstObservedAt;
    @Column(nullable = false) private Instant lastObservedAt;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private int sourceCount;
    @Column(nullable = false) private int uniqueSourceCount;
    @Column(nullable = false) private String algorithmVersion;
    @ElementCollection @CollectionTable(name = "social_signal_evidence", joinColumns = @JoinColumn(name = "signal_id"))
    private List<SocialSignalEvidenceValue> evidence = new ArrayList<>();
    protected SocialSignalEntity() { }
    SocialSignalEntity(SocialSignal signal) {
        id = signal.signalId(); tokenAddress = signal.tokenAddress(); signalType = signal.signalType(); strength = signal.strength(); confidence = signal.confidence();
        firstObservedAt = signal.firstObservedAt(); lastObservedAt = signal.lastObservedAt(); expiresAt = signal.expiresAt();
        sourceCount = signal.sourceCount(); uniqueSourceCount = signal.uniqueSourceCount(); algorithmVersion = signal.algorithmVersion();
        evidence = signal.evidence().stream().map(SocialSignalEvidenceValue::new).toList();
    }
    SocialSignal toDomain() { return new SocialSignal(id, tokenAddress, signalType, strength, confidence, firstObservedAt, lastObservedAt, expiresAt,
            sourceCount, uniqueSourceCount, evidence.stream().map(SocialSignalEvidenceValue::toDomain).toList(), algorithmVersion); }
}
