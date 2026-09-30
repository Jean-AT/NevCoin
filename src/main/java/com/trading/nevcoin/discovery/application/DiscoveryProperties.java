package com.trading.nevcoin.discovery.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "discovery")
public class DiscoveryProperties {

    private BigDecimal minimumLiquidityUsd = BigDecimal.valueOf(10_000);
    private BigDecimal minimumVolume24hUsd = BigDecimal.valueOf(25_000);
    private int minimumUniqueTraders5m;
    private long candidateTtlMinutes = 30;

    public BigDecimal getMinimumLiquidityUsd() { return minimumLiquidityUsd; }
    public BigDecimal getMinimumVolume24hUsd() { return minimumVolume24hUsd; }
    public int getMinimumUniqueTraders5m() { return minimumUniqueTraders5m; }
    public long getCandidateTtlMinutes() { return candidateTtlMinutes; }

    public void setMinimumLiquidityUsd(BigDecimal value) { this.minimumLiquidityUsd = value; }
    public void setMinimumVolume24hUsd(BigDecimal value) { this.minimumVolume24hUsd = value; }
    public void setMinimumUniqueTraders5m(int value) { this.minimumUniqueTraders5m = value; }
    public void setCandidateTtlMinutes(long value) { this.candidateTtlMinutes = value; }
}
