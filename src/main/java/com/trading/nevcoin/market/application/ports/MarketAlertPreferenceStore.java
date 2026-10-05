package com.trading.nevcoin.market.application.ports;

import java.util.Optional;

public interface MarketAlertPreferenceStore {

    Optional<Boolean> find(String tokenAddress);

    void save(String tokenAddress, boolean enabled);

    void deleteTokenOverrides();
}
