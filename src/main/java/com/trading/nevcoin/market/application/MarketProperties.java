package com.trading.nevcoin.market.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "market")
public class MarketProperties {

    private boolean enabled;
    private long pollIntervalMs = 30_000;
    private String birdeyeApiKey = "";
    private String birdeyeBaseUrl = "https://public-api.birdeye.so";
    private String chain = "solana";
    private String watchMints = "";
    private boolean alertsEnabled;
    private String alertChatIds = "";
    private BigDecimal momentumThresholdPercent = BigDecimal.valueOf(5);
    private BigDecimal volumeSpikeMultiplier = BigDecimal.valueOf(3);
    private BigDecimal liquidityDropThresholdPercent = BigDecimal.valueOf(20);

    public Set<String> parsedWatchMints() {
        return parseStrings(watchMints);
    }

    public Set<Long> parsedAlertChatIds() {
        return parseStrings(alertChatIds).stream().map(Long::valueOf).collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> parseStrings(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isEnabled() { return enabled; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public String getBirdeyeApiKey() { return birdeyeApiKey; }
    public String getBirdeyeBaseUrl() { return birdeyeBaseUrl; }
    public String getChain() { return chain; }
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public BigDecimal getMomentumThresholdPercent() { return momentumThresholdPercent; }
    public BigDecimal getVolumeSpikeMultiplier() { return volumeSpikeMultiplier; }
    public BigDecimal getLiquidityDropThresholdPercent() { return liquidityDropThresholdPercent; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    public void setBirdeyeApiKey(String birdeyeApiKey) { this.birdeyeApiKey = birdeyeApiKey; }
    public void setBirdeyeBaseUrl(String birdeyeBaseUrl) { this.birdeyeBaseUrl = birdeyeBaseUrl; }
    public void setChain(String chain) { this.chain = chain; }
    public void setWatchMints(String watchMints) { this.watchMints = watchMints; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }
    public void setAlertChatIds(String alertChatIds) { this.alertChatIds = alertChatIds; }
    public void setMomentumThresholdPercent(BigDecimal value) { this.momentumThresholdPercent = value; }
    public void setVolumeSpikeMultiplier(BigDecimal value) { this.volumeSpikeMultiplier = value; }
    public void setLiquidityDropThresholdPercent(BigDecimal value) { this.liquidityDropThresholdPercent = value; }
}
