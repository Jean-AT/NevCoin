package com.trading.nevcoin.social.infrastructure;

import com.trading.nevcoin.social.application.SocialProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SocialProperties.class)
public class SocialConfiguration {
}
