package com.trading.nevcoin.notification.application.ports;

public interface TokenWatchlistCommandPort {
    void watch(String mintAddress);
    void updateMetadata(String mintAddress, String symbol, String name);
    void unwatch(String mintAddress);
}
