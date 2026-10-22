package com.trading.nevcoin.social.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SocialEventJpaRepository extends JpaRepository<SocialEventEntity, String> {
    boolean existsByProviderAndSourceIdAndExternalId(String provider, String sourceId, String externalId);
    List<SocialEventEntity> findByObservedAtAfterOrderByObservedAtDesc(Instant since);
}
