package com.trading.nevcoin.social.application.ports;

import java.time.Instant;

public interface SocialProcessingFailureStore {
    void record(String sourceId, String stage, SocialProviderFailure failure, Instant failedAt);
}
