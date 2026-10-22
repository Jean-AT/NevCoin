package com.trading.nevcoin.social.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialCursorJpaRepository extends JpaRepository<SocialCursorEntity, String> {
}
