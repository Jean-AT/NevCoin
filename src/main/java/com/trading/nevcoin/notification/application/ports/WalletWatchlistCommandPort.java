package com.trading.nevcoin.notification.application.ports;

public interface WalletWatchlistCommandPort {
    void watch(String address);
    void unwatch(String address);
}
