package com.trading.nevcoin.signal.infrastructure.persistence;

import com.trading.nevcoin.signal.domain.SignalPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

interface CrossDomainSignalJpaRepository extends JpaRepository<CrossDomainSignalEntity, String> {
    List<CrossDomainSignalEntity> findByDetectedAtAfterOrderByDetectedAtDesc(Instant since);
    List<CrossDomainSignalEntity> findByTokenAddressAndExpiresAtGreaterThanEqualOrderByDetectedAtDesc(String tokenAddress, Instant asOf);
    List<CrossDomainSignalEntity> findByPriorityAndExpiresAtGreaterThanEqualOrderByDetectedAtDesc(SignalPriority priority, Instant asOf);
}
