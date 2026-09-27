package com.trading.nevcoin.market.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketSignalJpaRepository extends JpaRepository<MarketSignalEntity, String> {
}
