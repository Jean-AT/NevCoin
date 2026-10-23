package com.trading.nevcoin.signal.infrastructure;

import com.trading.nevcoin.signal.application.SignalIntelligenceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SignalIntelligenceProperties.class)
public class SignalConfiguration { }
