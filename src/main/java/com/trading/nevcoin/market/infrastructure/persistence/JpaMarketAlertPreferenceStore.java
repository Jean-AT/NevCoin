package com.trading.nevcoin.market.infrastructure.persistence;

import com.trading.nevcoin.market.application.ports.MarketAlertPreferenceStore;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaMarketAlertPreferenceStore implements MarketAlertPreferenceStore {

    private final MarketAlertPreferenceJpaRepository repository;

    public JpaMarketAlertPreferenceStore(MarketAlertPreferenceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Boolean> find(String tokenAddress) {
        return repository.findById(tokenAddress).map(MarketAlertPreferenceEntity::isEnabled);
    }

    @Override
    public void save(String tokenAddress, boolean enabled) {
        repository.save(new MarketAlertPreferenceEntity(tokenAddress, enabled));
    }

    @Override
    public void deleteTokenOverrides() {
        repository.deleteTokenOverrides();
    }
}
