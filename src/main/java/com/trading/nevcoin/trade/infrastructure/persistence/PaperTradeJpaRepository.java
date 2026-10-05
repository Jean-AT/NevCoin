package com.trading.nevcoin.trade.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaperTradeJpaRepository extends JpaRepository<PaperTradeEntity, String> {

    List<PaperTradeEntity> findTop10ByOrderByExecutedAtDesc();

    @Query("select coalesce(sum(t.realizedPnlUsd), 0) from PaperTradeEntity t "
            + "where t.action = com.trading.nevcoin.trade.domain.TradeAction.SELL "
            + "and t.executedAt >= :since")
    BigDecimal realizedPnlSince(@Param("since") Instant since);
}
