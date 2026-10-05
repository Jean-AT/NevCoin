package com.trading.nevcoin.trade.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "paper_settings")
public class PaperSettingsEntity {

    public static final String DEFAULT_ID = "default";

    @Id
    private String id;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "cash_usd", nullable = false, precision = 30, scale = 12)
    private BigDecimal cashUsd;

    @Column(name = "initial_cash_usd", nullable = false, precision = 30, scale = 12)
    private BigDecimal initialCashUsd;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaperSettingsEntity() {
    }

    public PaperSettingsEntity(boolean enabled, BigDecimal initialCashUsd) {
        this.id = DEFAULT_ID;
        this.enabled = enabled;
        this.cashUsd = initialCashUsd;
        this.initialCashUsd = initialCashUsd;
        this.updatedAt = Instant.now();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; this.updatedAt = Instant.now(); }
    public BigDecimal getCashUsd() { return cashUsd; }
    public void setCashUsd(BigDecimal value) { this.cashUsd = value; this.updatedAt = Instant.now(); }
    public BigDecimal getInitialCashUsd() { return initialCashUsd; }
}
