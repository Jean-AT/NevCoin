package com.trading.nevcoin.social.application.ports;

public record SocialProviderFailure(
        String stage,
        String category,
        String message,
        boolean retryable) {
}
