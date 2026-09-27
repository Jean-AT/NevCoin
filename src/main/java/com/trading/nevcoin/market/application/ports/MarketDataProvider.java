package com.trading.nevcoin.market.application.ports;

import com.trading.nevcoin.market.domain.MarketTick;

public interface MarketDataProvider {

    MarketTick fetch(String tokenAddress);
}
