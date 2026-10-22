package com.trading.nevcoin.social.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "social")
public class SocialProperties {
    private boolean enabled;
    private long pollIntervalMs = 120_000;
    private String rssUrls = "";
    private boolean alertsEnabled;
    private String alertChatIds = "";
    private int maxEventsPerFeed = 25;

    public boolean isEnabled() { return enabled; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public String getRssUrls() { return rssUrls; }
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public int getMaxEventsPerFeed() { return maxEventsPerFeed; }
    public Set<Long> parsedAlertChatIds() {
        if (alertChatIds == null || alertChatIds.isBlank()) return Set.of();
        return Arrays.stream(alertChatIds.split(",")).map(String::trim).filter(value -> !value.isBlank())
                .map(Long::valueOf).collect(Collectors.toUnmodifiableSet());
    }
    public void setEnabled(boolean value) { enabled = value; }
    public void setPollIntervalMs(long value) { pollIntervalMs = value; }
    public void setRssUrls(String value) { rssUrls = value; }
    public void setAlertsEnabled(boolean value) { alertsEnabled = value; }
    public void setAlertChatIds(String value) { alertChatIds = value; }
    public void setMaxEventsPerFeed(int value) { maxEventsPerFeed = value; }
}
