package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.application.ports.SocialEventStore;
import com.trading.nevcoin.social.domain.SocialEvent;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class JpaSocialEventStore implements SocialEventStore {
    private final SocialEventJpaRepository repository;

    public JpaSocialEventStore(SocialEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean saveIfNew(SocialEvent event) {
        if (repository.existsByProviderAndSourceIdAndExternalId(event.provider(), event.sourceId(), event.externalEventId())) return false;
        try {
            repository.saveAndFlush(new SocialEventEntity(event));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SocialEvent> recent(Instant since) {
        return repository.findByObservedAtAfterOrderByObservedAtDesc(since).stream()
                .map(SocialEventEntity::toDomain).toList();
    }
}
