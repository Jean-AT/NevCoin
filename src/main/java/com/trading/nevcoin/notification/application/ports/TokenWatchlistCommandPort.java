package com.trading.nevcoin.notification.application.ports;

public interface TokenWatchlistCommandPort {
    void watch(String mintAddress);
    void unwatch(String mintAddress);
}
