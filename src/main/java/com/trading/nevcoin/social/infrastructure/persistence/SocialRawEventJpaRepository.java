package com.trading.nevcoin.social.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SocialRawEventJpaRepository extends JpaRepository<SocialRawEventEntity, String> {
    boolean existsByProviderAndSourceIdAndExternalEventId(String provider, String sourceId, String externalEventId);
    List<SocialRawEventEntity> findByReceivedAtLessThanEqualOrderByReceivedAtAsc(Instant asOf);
}
