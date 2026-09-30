package com.trading.nevcoin.market.infrastructure;

import com.trading.nevcoin.market.application.MarketProperties;
import com.trading.nevcoin.discovery.application.DiscoveryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({MarketProperties.class, DiscoveryProperties.class})
public class MarketConfiguration {
}
