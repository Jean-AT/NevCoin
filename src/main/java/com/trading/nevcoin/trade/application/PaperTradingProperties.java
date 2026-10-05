package com.trading.nevcoin.trade.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "paper")
public class PaperTradingProperties {

    private boolean enabled;
    private long pollIntervalMs = 30_000;
    private BigDecimal initialCashUsd = BigDecimal.valueOf(1_000);
    private BigDecimal tradeNotionalUsd = BigDecimal.valueOf(25);
    private BigDecimal entryMomentumThresholdPercent = BigDecimal.valueOf(5);
    private BigDecimal exitMomentumThresholdPercent = BigDecimal.valueOf(-5);
    private boolean alertsEnabled = true;
    private String alertChatIds = "";
    private boolean notifyHoldDecisions;
    private long holdAlertIntervalMs = 300_000;
    private int maxOpenPositions = 5;
    private BigDecimal maxDailyLossUsd = BigDecimal.valueOf(100);
    private BigDecimal maxTradeNotionalUsd = BigDecimal.valueOf(25);
    private BigDecimal stopLossPercent = BigDecimal.valueOf(10);
    private BigDecimal takeProfitPercent = BigDecimal.valueOf(25);

    public boolean isEnabled() { return enabled; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public BigDecimal getInitialCashUsd() { return initialCashUsd; }
    public BigDecimal getTradeNotionalUsd() { return tradeNotionalUsd; }
    public BigDecimal getEntryMomentumThresholdPercent() { return entryMomentumThresholdPercent; }
    public BigDecimal getExitMomentumThresholdPercent() { return exitMomentumThresholdPercent; }
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public Set<Long> parsedAlertChatIds() {
        if (alertChatIds == null || alertChatIds.isBlank()) return Set.of();
        return Arrays.stream(alertChatIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }
    public boolean isNotifyHoldDecisions() { return notifyHoldDecisions; }
    public long getHoldAlertIntervalMs() { return holdAlertIntervalMs; }
    public int getMaxOpenPositions() { return maxOpenPositions; }
    public BigDecimal getMaxDailyLossUsd() { return maxDailyLossUsd; }
    public BigDecimal getMaxTradeNotionalUsd() { return maxTradeNotionalUsd; }
    public BigDecimal getStopLossPercent() { return stopLossPercent; }
    public BigDecimal getTakeProfitPercent() { return takeProfitPercent; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setPollIntervalMs(long value) { this.pollIntervalMs = value; }
    public void setInitialCashUsd(BigDecimal value) { this.initialCashUsd = value; }
    public void setTradeNotionalUsd(BigDecimal value) { this.tradeNotionalUsd = value; }
    public void setEntryMomentumThresholdPercent(BigDecimal value) { this.entryMomentumThresholdPercent = value; }
    public void setExitMomentumThresholdPercent(BigDecimal value) { this.exitMomentumThresholdPercent = value; }
    public void setAlertsEnabled(boolean value) { this.alertsEnabled = value; }
    public void setAlertChatIds(String value) { this.alertChatIds = value; }
    public void setNotifyHoldDecisions(boolean value) { this.notifyHoldDecisions = value; }
    public void setHoldAlertIntervalMs(long value) { this.holdAlertIntervalMs = value; }
    public void setMaxOpenPositions(int value) { this.maxOpenPositions = value; }
    public void setMaxDailyLossUsd(BigDecimal value) { this.maxDailyLossUsd = value; }
    public void setMaxTradeNotionalUsd(BigDecimal value) { this.maxTradeNotionalUsd = value; }
    public void setStopLossPercent(BigDecimal value) { this.stopLossPercent = value; }
    public void setTakeProfitPercent(BigDecimal value) { this.takeProfitPercent = value; }
}
