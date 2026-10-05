package com.trading.nevcoin.market.infrastructure.persistence;

import com.trading.nevcoin.market.domain.MarketSignal;
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
import java.util.UUID;

@Entity
@Table(name = "market_signals")
public class MarketSignalEntity {

    @Id
    private String id;
    @Column(nullable = false)
    private String tokenAddress;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketSignal.SignalType type;
    @Column(precision = 30, scale = 12, nullable = false)
    private BigDecimal strength;
    @Column(nullable = false)
    private Instant observedAt;
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private String sourceQuality;
    @ElementCollection
    @CollectionTable(name = "market_signal_evidence", joinColumns = @JoinColumn(name = "signal_id"))
    @Column(name = "evidence", nullable = false)
    private List<String> evidence = new ArrayList<>();

    protected MarketSignalEntity() {
    }

    public MarketSignalEntity(MarketSignal signal) {
        this.id = UUID.randomUUID().toString();
        this.tokenAddress = signal.tokenAddress();
        this.type = signal.type();
        this.strength = signal.strength();
        this.observedAt = signal.observedAt();
        this.expiresAt = signal.expiresAt();
        this.sourceQuality = signal.sourceQuality();
        this.evidence = new ArrayList<>(signal.evidence());
    }

    public MarketSignal.SignalType getType() { return type; }
    public String getTokenAddress() { return tokenAddress; }
    public BigDecimal getStrength() { return strength; }
    public Instant getObservedAt() { return observedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public List<String> getEvidence() { return List.copyOf(evidence); }
}
