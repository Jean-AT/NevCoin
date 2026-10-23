package com.trading.nevcoin.signal.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "signal.intelligence")
public class SignalIntelligenceProperties {
    private Duration correlationWindow = Duration.ofMinutes(10);
    private Duration expiry = Duration.ofMinutes(15);
    public Duration getCorrelationWindow() { return correlationWindow; }
    public void setCorrelationWindow(Duration correlationWindow) { this.correlationWindow = correlationWindow; }
    public Duration getExpiry() { return expiry; }
    public void setExpiry(Duration expiry) { this.expiry = expiry; }
}
