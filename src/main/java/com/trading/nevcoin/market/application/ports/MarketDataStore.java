package com.trading.nevcoin.market.application.ports;

import com.trading.nevcoin.market.domain.MarketSignal;
import com.trading.nevcoin.market.domain.MarketSnapshot;
import com.trading.nevcoin.market.domain.MarketTick;

import java.util.List;
import java.time.Instant;

public interface MarketDataStore {

    void saveTick(MarketTick tick);

    List<MarketTick> recentTicks(String tokenAddress, int limit);

    void saveSnapshot(MarketSnapshot snapshot);

    void saveSignal(MarketSignal signal);

    boolean hasActiveSignal(String tokenAddress, MarketSignal.SignalType type, Instant observedAt);
}
