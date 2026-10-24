package com.trading.nevcoin.notification.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "notification.outbox")
public class NotificationOutboxProperties {
    private boolean enabled = true;
    private int batchSize = 50;
    private Duration pollInterval = Duration.ofSeconds(5);
    private Duration lease = Duration.ofSeconds(30);
    private Duration baseBackoff = Duration.ofSeconds(5);
    private Duration maxBackoff = Duration.ofMinutes(5);
    private int maxAttempts = 5;

    public boolean isEnabled() { return enabled; }
    public int getBatchSize() { return batchSize; }
    public Duration getPollInterval() { return pollInterval; }
    public Duration getLease() { return lease; }
    public Duration getBaseBackoff() { return baseBackoff; }
    public Duration getMaxBackoff() { return maxBackoff; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public void setPollInterval(Duration pollInterval) { this.pollInterval = pollInterval; }
    public void setLease(Duration lease) { this.lease = lease; }
    public void setBaseBackoff(Duration baseBackoff) { this.baseBackoff = baseBackoff; }
    public void setMaxBackoff(Duration maxBackoff) { this.maxBackoff = maxBackoff; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
}
