package com.trading.nevcoin.notification.infrastructure.telegram;

import com.trading.nevcoin.notification.application.ports.SystemStatusPort;
import com.trading.nevcoin.notification.domain.TelegramAccessPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfiguration {

    @Bean
    TelegramAccessPolicy telegramAccessPolicy(TelegramProperties properties) {
        return new TelegramAccessPolicy(properties.parsedAllowedChatIds());
    }

    @Bean
    SystemStatusPort systemStatusPort(HealthEndpoint healthEndpoint) {
        return () -> new SystemStatusPort.SystemStatus(
                healthEndpoint.health().getStatus().getCode(),
                "paper-intelligence");
    }
}
