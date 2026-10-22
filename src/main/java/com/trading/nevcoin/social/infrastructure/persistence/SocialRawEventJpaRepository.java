package com.trading.nevcoin.social.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialRawEventJpaRepository extends JpaRepository<SocialRawEventEntity, String> {
    boolean existsByProviderAndSourceIdAndExternalEventId(String provider, String sourceId, String externalEventId);
}
