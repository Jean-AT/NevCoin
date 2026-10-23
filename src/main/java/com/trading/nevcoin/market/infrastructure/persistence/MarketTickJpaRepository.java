package com.trading.nevcoin.market.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;

public interface MarketTickJpaRepository extends JpaRepository<MarketTickEntity, String> {

    List<MarketTickEntity> findTop32ByTokenAddressOrderByObservedAtDesc(String tokenAddress);
    List<MarketTickEntity> findByObservedAtLessThanEqualOrderByObservedAtAsc(Instant asOf);
}
