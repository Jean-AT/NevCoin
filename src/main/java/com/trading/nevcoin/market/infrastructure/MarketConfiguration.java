package com.trading.nevcoin.market.infrastructure;

import com.trading.nevcoin.market.application.MarketProperties;
import com.trading.nevcoin.discovery.application.DiscoveryProperties;
import com.trading.nevcoin.trade.application.PaperTradingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({MarketProperties.class, DiscoveryProperties.class, PaperTradingProperties.class})
public class MarketConfiguration {
}
