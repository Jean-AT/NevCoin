package com.trading.nevcoin.market.infrastructure.persistence;

import com.trading.nevcoin.market.domain.MarketSnapshot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "market_snapshots")
public class MarketSnapshotEntity {

    @Id
    private String id;
    @Column(nullable = false)
    private String tokenAddress;
    @Column(nullable = false)
    private Instant timestamp;
    @Column(precision = 30, scale = 12)
    private BigDecimal priceUsd;
    @Column(precision = 30, scale = 12)
    private BigDecimal liquidityUsd;
    @Column(precision = 30, scale = 12)
    private BigDecimal volume24hUsd;
    @Column(precision = 30, scale = 12)
    private BigDecimal priceChange1m;
    @Column(precision = 30, scale = 12)
    private BigDecimal priceChange5m;
    @Column(precision = 30, scale = 12)
    private BigDecimal priceChange15m;
    @Column(precision = 30, scale = 12)
    private BigDecimal volatility5m;
    @Column(nullable = false)
    private long dataFreshnessMs;

    protected MarketSnapshotEntity() {
    }

    public MarketSnapshotEntity(MarketSnapshot snapshot) {
        this.id = UUID.randomUUID().toString();
        this.tokenAddress = snapshot.tokenAddress();
        this.timestamp = snapshot.timestamp();
        this.priceUsd = snapshot.priceUsd();
        this.liquidityUsd = snapshot.liquidityUsd();
        this.volume24hUsd = snapshot.volume24hUsd();
        this.priceChange1m = snapshot.priceChange1m();
        this.priceChange5m = snapshot.priceChange5m();
        this.priceChange15m = snapshot.priceChange15m();
        this.volatility5m = snapshot.volatility5m();
        this.dataFreshnessMs = snapshot.dataFreshnessMs();
    }
}
