package com.trading.nevcoin.market.infrastructure;

import com.trading.nevcoin.market.application.MarketProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MarketProperties.class)
public class MarketConfiguration {
}
