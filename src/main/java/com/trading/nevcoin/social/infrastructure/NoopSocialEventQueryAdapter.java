package com.trading.nevcoin.social.infrastructure;

import com.trading.nevcoin.social.application.ports.SocialEventQueryPort;
import com.trading.nevcoin.social.domain.SocialEvent;

import java.time.Instant;
import java.util.List;

/** Safe default until an explicitly configured social provider is enabled. */
public class NoopSocialEventQueryAdapter implements SocialEventQueryPort {
    @Override
    public List<SocialEvent> recent(Instant since) {
        return List.of();
    }
}
