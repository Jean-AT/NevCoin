package com.trading.nevcoin.market.infrastructure.persistence;

import com.trading.nevcoin.market.domain.MarketTick;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "market_ticks")
public class MarketTickEntity {

    @Id
    private String id;
    @Column(nullable = false)
    private String tokenAddress;
    @Column(nullable = false)
    private Instant observedAt;
    @Column(nullable = false)
    private Instant sourceTimestamp;
    @Column(precision = 30, scale = 12)
    private BigDecimal priceUsd;
    @Column(precision = 30, scale = 12)
    private BigDecimal liquidityUsd;
    @Column(precision = 30, scale = 12)
    private BigDecimal volume24hUsd;
    @Column(precision = 30, scale = 12)
    private BigDecimal priceChange24hPercent;
    @Column(nullable = false)
    private String sourceQuality;

    protected MarketTickEntity() {
    }

    public MarketTickEntity(MarketTick tick) {
        this.id = UUID.randomUUID().toString();
        this.tokenAddress = tick.tokenAddress();
        this.observedAt = tick.observedAt();
        this.sourceTimestamp = tick.sourceTimestamp();
        this.priceUsd = tick.priceUsd();
        this.liquidityUsd = tick.liquidityUsd();
        this.volume24hUsd = tick.volume24hUsd();
        this.priceChange24hPercent = tick.priceChange24hPercent();
        this.sourceQuality = tick.sourceQuality();
    }

    public MarketTick toDomain() {
        return new MarketTick(tokenAddress, observedAt, sourceTimestamp, priceUsd, liquidityUsd, volume24hUsd,
                priceChange24hPercent, sourceQuality);
    }
}
