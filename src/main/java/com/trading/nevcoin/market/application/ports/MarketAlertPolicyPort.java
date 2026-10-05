package com.trading.nevcoin.market.application.ports;

public interface MarketAlertPolicyPort {

    boolean isEnabledFor(String tokenAddress);
}
