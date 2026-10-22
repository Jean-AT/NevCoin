package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.application.ports.SocialRawEventStore;
import com.trading.nevcoin.social.domain.RawSocialEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaSocialRawEventStore implements SocialRawEventStore {
    private final SocialRawEventJpaRepository repository;

    public JpaSocialRawEventStore(SocialRawEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean saveIfNew(RawSocialEvent event) {
        if (repository.existsByProviderAndSourceIdAndExternalEventId(event.provider(), event.sourceId(), event.externalEventId())) {
            return false;
        }
        repository.save(new SocialRawEventEntity(event));
        return true;
    }
}
