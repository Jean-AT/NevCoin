package com.trading.nevcoin.market.infrastructure.dexscreener;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "market.overview")
public class DexScreenerProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.dexscreener.com";
    private String chainId = "solana";
    private int timeoutSeconds = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getChainId() {
        return chainId;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
