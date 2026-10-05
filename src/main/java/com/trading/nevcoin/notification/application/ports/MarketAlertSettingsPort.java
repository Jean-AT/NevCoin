package com.trading.nevcoin.notification.application.ports;

public interface MarketAlertSettingsPort {

    void setAll(boolean enabled);

    void setToken(String mintAddress, boolean enabled);

    boolean defaultEnabled();

    boolean isEnabledFor(String mintAddress);
}
