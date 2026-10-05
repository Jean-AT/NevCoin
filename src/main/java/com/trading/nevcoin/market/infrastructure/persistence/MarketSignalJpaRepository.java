package com.trading.nevcoin.market.infrastructure.persistence;

import com.trading.nevcoin.market.domain.MarketSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MarketSignalJpaRepository extends JpaRepository<MarketSignalEntity, String> {

    boolean existsByTokenAddressAndTypeAndExpiresAtAfter(
            String tokenAddress,
            MarketSignal.SignalType type,
            Instant observedAt);

    List<MarketSignalEntity> findByExpiresAtAfterOrderByObservedAtDesc(Instant now);
}
