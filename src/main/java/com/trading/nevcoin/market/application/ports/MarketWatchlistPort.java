package com.trading.nevcoin.market.application.ports;

import java.util.Set;

public interface MarketWatchlistPort {

    Set<String> activeMintAddresses();
}
