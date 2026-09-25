package com.trading.nevcoin.notification.infrastructure.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private boolean enabled;
    private String botToken = "";
    private String allowedChatIds = "";
    private String apiBaseUrl = "https://api.telegram.org";
    private int longPollTimeoutSeconds = 30;
    private int errorBackoffSeconds = 5;

    public Set<Long> parsedAllowedChatIds() {
        if (allowedChatIds == null || allowedChatIds.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(allowedChatIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::parseChatId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private Long parseChatId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("TELEGRAM_ALLOWED_CHAT_IDS must contain numeric chat IDs", exception);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public int getLongPollTimeoutSeconds() {
        return longPollTimeoutSeconds;
    }

    public int getErrorBackoffSeconds() {
        return errorBackoffSeconds;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public void setAllowedChatIds(String allowedChatIds) {
        this.allowedChatIds = allowedChatIds;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public void setLongPollTimeoutSeconds(int longPollTimeoutSeconds) {
        this.longPollTimeoutSeconds = longPollTimeoutSeconds;
    }

    public void setErrorBackoffSeconds(int errorBackoffSeconds) {
        this.errorBackoffSeconds = errorBackoffSeconds;
    }
}
