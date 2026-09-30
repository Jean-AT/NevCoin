package com.trading.nevcoin.wallet.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TrackedWalletJpaRepository extends JpaRepository<TrackedWalletEntity, String> { }
