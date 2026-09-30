package com.trading.nevcoin.wallet.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wallet")
public class WalletProperties {
    private boolean enabled;
    private long pollIntervalMs = 60_000;
    private boolean alertsEnabled;
    private String alertChatIds = "";
    private String heliusRpcUrl = "https://mainnet.helius-rpc.com";
    private String heliusApiKey = "";

    public String getHeliusRpcUrl() { return heliusRpcUrl; }
    public boolean isEnabled() { return enabled; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public java.util.Set<Long> parsedAlertChatIds() {
        if (alertChatIds == null || alertChatIds.isBlank()) return java.util.Set.of();
        return java.util.Arrays.stream(alertChatIds.split(",")).map(String::trim).filter(value -> !value.isBlank())
                .map(Long::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public String getHeliusApiKey() { return heliusApiKey; }
    public void setHeliusRpcUrl(String heliusRpcUrl) { this.heliusRpcUrl = heliusRpcUrl; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }
    public void setAlertChatIds(String alertChatIds) { this.alertChatIds = alertChatIds; }
    public void setHeliusApiKey(String heliusApiKey) { this.heliusApiKey = heliusApiKey; }
}
