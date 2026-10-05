package com.trading.nevcoin.market.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketSnapshotJpaRepository extends JpaRepository<MarketSnapshotEntity, String> {

    java.util.Optional<MarketSnapshotEntity> findTopByTokenAddressOrderByTimestampDesc(String tokenAddress);
}
