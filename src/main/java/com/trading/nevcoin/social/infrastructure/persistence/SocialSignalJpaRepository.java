package com.trading.nevcoin.social.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

interface SocialSignalJpaRepository extends JpaRepository<SocialSignalEntity, String> {
    List<SocialSignalEntity> findByLastObservedAtAfterOrderByLastObservedAtDesc(Instant since);
    List<SocialSignalEntity> findByTokenAddressAndExpiresAtGreaterThanEqualOrderByLastObservedAtDesc(String tokenAddress, Instant asOf);
}
