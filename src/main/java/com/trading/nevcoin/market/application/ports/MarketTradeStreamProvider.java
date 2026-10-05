package com.trading.nevcoin.market.application.ports;

import com.trading.nevcoin.market.domain.MarketTrade;

import java.util.Set;
import java.util.function.Consumer;

public interface MarketTradeStreamProvider {

    void start(Set<String> tokenAddresses, Consumer<MarketTrade> consumer);

    void updateSubscriptions(Set<String> tokenAddresses);

    void stop();
}
