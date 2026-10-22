package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.application.ports.SocialCursorStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
public class JpaSocialCursorStore implements SocialCursorStore {
    private final SocialCursorJpaRepository repository;

    public JpaSocialCursorStore(SocialCursorJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> current(String sourceId) {
        return repository.findById(sourceId).map(SocialCursorEntity::getCursor);
    }

    @Override
    @Transactional
    public void advance(String sourceId, String cursor, Instant fetchedAt) {
        repository.save(new SocialCursorEntity(sourceId, cursor, fetchedAt));
    }
}
