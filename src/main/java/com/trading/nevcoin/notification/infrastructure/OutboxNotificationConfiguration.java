package com.trading.nevcoin.notification.infrastructure;

import com.trading.nevcoin.notification.application.NotificationOutboxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationOutboxProperties.class)
public class OutboxNotificationConfiguration {
}
