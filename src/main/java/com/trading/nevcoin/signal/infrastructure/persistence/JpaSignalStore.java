package com.trading.nevcoin.signal.infrastructure.persistence;

import com.trading.nevcoin.signal.application.ports.SignalStore;
import com.trading.nevcoin.signal.domain.CrossDomainSignal;
import com.trading.nevcoin.signal.domain.SignalPriority;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Component
public class JpaSignalStore implements SignalStore {
    private final CrossDomainSignalJpaRepository repository;
    public JpaSignalStore(CrossDomainSignalJpaRepository repository) { this.repository = repository; }
    @Override @Transactional public boolean saveIfNew(CrossDomainSignal signal) { try { repository.saveAndFlush(new CrossDomainSignalEntity(signal)); return true; } catch (DataIntegrityViolationException duplicate) { return false; } }
    @Override @Transactional(readOnly = true) public List<CrossDomainSignal> recent(Instant since) { return repository.findByDetectedAtAfterOrderByDetectedAtDesc(since).stream().map(CrossDomainSignalEntity::toDomain).toList(); }
    @Override @Transactional(readOnly = true) public List<CrossDomainSignal> activeForToken(String tokenAddress, Instant asOf) { return repository.findByTokenAddressAndExpiresAtGreaterThanEqualOrderByDetectedAtDesc(tokenAddress, asOf).stream().map(CrossDomainSignalEntity::toDomain).toList(); }
    @Override @Transactional(readOnly = true) public List<CrossDomainSignal> activeWithPriority(SignalPriority priority, Instant asOf) { return repository.findByPriorityAndExpiresAtGreaterThanEqualOrderByDetectedAtDesc(priority, asOf).stream().map(CrossDomainSignalEntity::toDomain).toList(); }
}
