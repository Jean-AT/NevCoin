package com.trading.nevcoin.signal.infrastructure.persistence;

import com.trading.nevcoin.signal.domain.SignalPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface CrossDomainSignalJpaRepository extends JpaRepository<CrossDomainSignalEntity, String> {
    List<CrossDomainSignalEntity> findByDetectedAtLessThanEqualOrderByDetectedAtAsc(Instant asOf);
    List<CrossDomainSignalEntity> findByDetectedAtAfterOrderByDetectedAtDesc(Instant since);
    List<CrossDomainSignalEntity> findByTokenAddressAndExpiresAtGreaterThanEqualOrderByDetectedAtDesc(String tokenAddress, Instant asOf);
    List<CrossDomainSignalEntity> findByPriorityAndExpiresAtGreaterThanEqualOrderByDetectedAtDesc(SignalPriority priority, Instant asOf);
}
