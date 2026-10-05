package com.trading.nevcoin.market.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MarketAlertPreferenceJpaRepository
        extends JpaRepository<MarketAlertPreferenceEntity, String> {

    @Modifying
    @Query("delete from MarketAlertPreferenceEntity preference where preference.tokenAddress <> '*'")
    void deleteTokenOverrides();
}
