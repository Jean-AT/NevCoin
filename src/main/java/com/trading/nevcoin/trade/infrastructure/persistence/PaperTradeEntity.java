package com.trading.nevcoin.trade.infrastructure.persistence;

import com.trading.nevcoin.trade.domain.PaperTrade;
import com.trading.nevcoin.trade.domain.TradeAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "paper_trades")
public class PaperTradeEntity {

    @Id
    private String id;

    @Column(name = "token_address", nullable = false, length = 100)
    private String tokenAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeAction action;

    @Column(nullable = false, precision = 30, scale = 18)
    private BigDecimal quantity;

    @Column(name = "price_usd", nullable = false, precision = 30, scale = 12)
    private BigDecimal priceUsd;

    @Column(name = "notional_usd", nullable = false, precision = 30, scale = 12)
    private BigDecimal notionalUsd;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "realized_pnl_usd", precision = 30, scale = 12)
    private BigDecimal realizedPnlUsd;

    protected PaperTradeEntity() {
    }

    public PaperTradeEntity(PaperTrade trade) {
        this.id = UUID.randomUUID().toString();
        this.tokenAddress = trade.tokenAddress();
        this.action = trade.action();
        this.quantity = trade.quantity();
        this.priceUsd = trade.priceUsd();
        this.notionalUsd = trade.notionalUsd();
        this.reason = trade.reason();
        this.executedAt = trade.executedAt();
        this.realizedPnlUsd = trade.realizedPnlUsd();
    }

    public PaperTrade toDomain() {
        return new PaperTrade(tokenAddress, action, quantity, priceUsd, notionalUsd, reason, executedAt, realizedPnlUsd);
    }
}
