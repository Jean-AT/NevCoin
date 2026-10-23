package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.application.ports.SocialSignalStore;
import com.trading.nevcoin.social.domain.SocialSignal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Component
public class JpaSocialSignalStore implements SocialSignalStore {
    private final SocialSignalJpaRepository repository;
    public JpaSocialSignalStore(SocialSignalJpaRepository repository) { this.repository = repository; }
    @Override @Transactional public boolean saveIfNew(SocialSignal signal) { try { repository.saveAndFlush(new SocialSignalEntity(signal)); return true; } catch (DataIntegrityViolationException duplicate) { return false; } }
    @Override @Transactional(readOnly = true) public List<SocialSignal> recent(Instant since) { return repository.findByLastObservedAtAfterOrderByLastObservedAtDesc(since).stream().map(SocialSignalEntity::toDomain).toList(); }
    @Override @Transactional(readOnly = true) public List<SocialSignal> activeForToken(String tokenAddress, Instant asOf) { return repository.findByTokenAddressAndExpiresAtGreaterThanEqualOrderByLastObservedAtDesc(tokenAddress, asOf).stream().map(SocialSignalEntity::toDomain).toList(); }
}
