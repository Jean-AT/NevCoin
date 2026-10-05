package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.application.ports.MarketAlertPolicyPort;
import com.trading.nevcoin.market.application.ports.MarketAlertPreferenceStore;
import com.trading.nevcoin.notification.application.ports.MarketAlertSettingsPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketAlertPreferencesService implements MarketAlertPolicyPort, MarketAlertSettingsPort {

    static final String GLOBAL_PREFERENCE = "*";

    private final MarketAlertPreferenceStore store;
    private final MarketProperties properties;

    public MarketAlertPreferencesService(MarketAlertPreferenceStore store, MarketProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void setAll(boolean enabled) {
        store.deleteTokenOverrides();
        store.save(GLOBAL_PREFERENCE, enabled);
    }

    @Override
    @Transactional
    public void setToken(String mintAddress, boolean enabled) {
        store.save(mintAddress, enabled);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean defaultEnabled() {
        return store.find(GLOBAL_PREFERENCE).orElse(properties.isAlertsEnabled());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabledFor(String tokenAddress) {
        return store.find(tokenAddress).orElseGet(this::defaultEnabled);
    }
}
