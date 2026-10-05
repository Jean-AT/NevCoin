package com.trading.nevcoin.market.application;

import com.trading.nevcoin.market.application.ports.MarketAlertPreferenceStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketAlertPreferencesServiceTest {

    @Test
    void supportsGlobalDefaultAndPerTokenOverride() {
        MarketProperties properties = new MarketProperties();
        properties.setAlertsEnabled(true);
        InMemoryStore store = new InMemoryStore();
        MarketAlertPreferencesService service = new MarketAlertPreferencesService(store, properties);

        assertTrue(service.isEnabledFor("mint-a"));

        service.setToken("mint-a", false);
        assertFalse(service.isEnabledFor("mint-a"));
        assertTrue(service.isEnabledFor("mint-b"));

        service.setAll(false);
        assertFalse(service.isEnabledFor("mint-a"));
        assertFalse(service.isEnabledFor("mint-b"));
    }

    private static class InMemoryStore implements MarketAlertPreferenceStore {
        private final Map<String, Boolean> values = new HashMap<>();

        @Override public Optional<Boolean> find(String tokenAddress) {
            return Optional.ofNullable(values.get(tokenAddress));
        }
        @Override public void save(String tokenAddress, boolean enabled) {
            values.put(tokenAddress, enabled);
        }
        @Override public void deleteTokenOverrides() {
            values.keySet().removeIf(key -> !MarketAlertPreferencesService.GLOBAL_PREFERENCE.equals(key));
        }
    }
}
