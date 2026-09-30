package com.trading.nevcoin.discovery.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenJpaRepository extends JpaRepository<TokenEntity, String> {
    Optional<TokenEntity> findBySymbolIgnoreCase(String symbol);
}
