package com.trading.nevcoin.wallet.infrastructure;

import com.trading.nevcoin.wallet.application.WalletProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WalletProperties.class)
public class WalletConfiguration { }
