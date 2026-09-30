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
    @Column(precision = 30, scale = 12)
    private BigDecimal buyVolume5m;
    @Column(precision = 30, scale = 12)
    private BigDecimal sellVolume5m;
    @Column(precision = 30, scale = 12)
    private BigDecimal netFlow5m;
    @Column(nullable = false)
    private int uniqueBuyers5m;
    @Column(nullable = false)
    private int uniqueSellers5m;

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
        this.buyVolume5m = tick.buyVolume5m();
        this.sellVolume5m = tick.sellVolume5m();
        this.netFlow5m = tick.netFlow5m();
        this.uniqueBuyers5m = tick.uniqueBuyers5m();
        this.uniqueSellers5m = tick.uniqueSellers5m();
    }

    public MarketTick toDomain() {
        return new MarketTick(tokenAddress, observedAt, sourceTimestamp, priceUsd, liquidityUsd, volume24hUsd,
                priceChange24hPercent, sourceQuality, buyVolume5m, sellVolume5m, netFlow5m,
                uniqueBuyers5m, uniqueSellers5m);
    }
}
