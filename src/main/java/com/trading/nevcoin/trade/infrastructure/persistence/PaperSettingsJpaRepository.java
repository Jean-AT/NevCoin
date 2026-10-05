package com.trading.nevcoin.trade.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperSettingsJpaRepository extends JpaRepository<PaperSettingsEntity, String> {
}
