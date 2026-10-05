package com.trading.nevcoin.trade.infrastructure.persistence;

import com.trading.nevcoin.trade.domain.PaperPosition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "paper_positions")
public class PaperPositionEntity {

    @Id
    @Column(name = "token_address", length = 100)
    private String tokenAddress;

    @Column(nullable = false, precision = 30, scale = 18)
    private BigDecimal quantity;

    @Column(name = "average_entry_price_usd", nullable = false, precision = 30, scale = 12)
    private BigDecimal averageEntryPriceUsd;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaperPositionEntity() {
    }

    public PaperPositionEntity(PaperPosition position) {
        this.tokenAddress = position.tokenAddress();
        this.quantity = position.quantity();
        this.averageEntryPriceUsd = position.averageEntryPriceUsd();
        this.updatedAt = position.updatedAt();
    }

    public PaperPosition toDomain() {
        return new PaperPosition(tokenAddress, quantity, averageEntryPriceUsd, updatedAt);
    }
}
