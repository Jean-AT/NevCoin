package com.trading.nevcoin.market.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "market_alert_preferences")
public class MarketAlertPreferenceEntity {

    @Id
    @Column(name = "token_address", nullable = false, length = 100)
    private String tokenAddress;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MarketAlertPreferenceEntity() {
    }

    public MarketAlertPreferenceEntity(String tokenAddress, boolean enabled) {
        this.tokenAddress = tokenAddress;
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
