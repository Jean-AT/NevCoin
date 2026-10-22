package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.application.ports.SocialProcessingFailureStore;
import com.trading.nevcoin.social.application.ports.SocialProviderFailure;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class JpaSocialProcessingFailureStore implements SocialProcessingFailureStore {
    private final SocialProcessingFailureJpaRepository repository;

    public JpaSocialProcessingFailureStore(SocialProcessingFailureJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void record(String sourceId, String stage, SocialProviderFailure failure, Instant failedAt) {
        repository.save(new SocialProcessingFailureEntity(sourceId, stage, failure, failedAt));
    }
}
