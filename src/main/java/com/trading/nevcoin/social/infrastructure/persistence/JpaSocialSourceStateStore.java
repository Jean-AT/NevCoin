package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.application.ports.SocialProviderFailure;
import com.trading.nevcoin.social.application.ports.SocialSourceStateStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaSocialSourceStateStore implements SocialSourceStateStore {
    private final SocialSourceStateJpaRepository repository;

    public JpaSocialSourceStateStore(SocialSourceStateJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<State> find(String sourceId) {
        return repository.findById(sourceId).map(SocialSourceStateEntity::toState);
    }

    @Override
    @Transactional
    public void markAttempt(String sourceId, Instant attemptedAt) {
        SocialSourceStateEntity state = repository.findById(sourceId).orElseGet(() -> new SocialSourceStateEntity(sourceId));
        state.markAttempt(attemptedAt);
        repository.save(state);
    }

    @Override
    @Transactional
    public void markSuccess(String sourceId, Instant completedAt, long latencyMs) {
        SocialSourceStateEntity state = repository.findById(sourceId).orElseGet(() -> new SocialSourceStateEntity(sourceId));
        state.markSuccess(completedAt, latencyMs);
        repository.save(state);
    }

    @Override
    @Transactional
    public void markFailure(String sourceId, Instant failedAt, SocialProviderFailure failure, Duration backoff,
                            long latencyMs) {
        SocialSourceStateEntity state = repository.findById(sourceId).orElseGet(() -> new SocialSourceStateEntity(sourceId));
        state.markFailure(failedAt, failure, failedAt.plus(backoff), latencyMs);
        repository.save(state);
    }

    @Override
    @Transactional(readOnly = true)
    public List<State> list() {
        return repository.findAll().stream().map(SocialSourceStateEntity::toState).toList();
    }
}
