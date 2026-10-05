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
    private boolean streamEnabled;
    private String streamUrl = "wss://mainnet.helius-rpc.com";
    private String streamChannel = "logsSubscribe";
    private long streamReconnectDelaySeconds = 5;
    private long watchlistRefreshMs = 5_000;
    private String heliusApiKey = "";
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
    public boolean isStreamEnabled() { return streamEnabled; }
    public String getStreamUrl() { return streamUrl; }
    public String getStreamChannel() { return streamChannel; }
    public long getStreamReconnectDelaySeconds() { return streamReconnectDelaySeconds; }
    public long getWatchlistRefreshMs() { return watchlistRefreshMs; }
    public String getHeliusApiKey() { return heliusApiKey; }
    public String getChain() { return chain; }
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public BigDecimal getMomentumThresholdPercent() { return momentumThresholdPercent; }
    public BigDecimal getVolumeSpikeMultiplier() { return volumeSpikeMultiplier; }
    public BigDecimal getLiquidityDropThresholdPercent() { return liquidityDropThresholdPercent; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    public void setStreamEnabled(boolean streamEnabled) { this.streamEnabled = streamEnabled; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }
    public void setStreamChannel(String streamChannel) { this.streamChannel = streamChannel; }
    public void setStreamReconnectDelaySeconds(long streamReconnectDelaySeconds) { this.streamReconnectDelaySeconds = streamReconnectDelaySeconds; }
    public void setWatchlistRefreshMs(long watchlistRefreshMs) { this.watchlistRefreshMs = watchlistRefreshMs; }
    public void setHeliusApiKey(String heliusApiKey) { this.heliusApiKey = heliusApiKey; }
    public void setChain(String chain) { this.chain = chain; }
    public void setWatchMints(String watchMints) { this.watchMints = watchMints; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }
    public void setAlertChatIds(String alertChatIds) { this.alertChatIds = alertChatIds; }
    public void setMomentumThresholdPercent(BigDecimal value) { this.momentumThresholdPercent = value; }
    public void setVolumeSpikeMultiplier(BigDecimal value) { this.volumeSpikeMultiplier = value; }
    public void setLiquidityDropThresholdPercent(BigDecimal value) { this.liquidityDropThresholdPercent = value; }
}
