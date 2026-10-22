package com.trading.nevcoin.social.application.ports;

import com.trading.nevcoin.social.domain.SocialEvent;

import java.time.Instant;
import java.util.List;

public interface SocialEventStore extends SocialEventQueryPort {
    /** Saves once; returns true only when this source/event id was new. */
    boolean saveIfNew(SocialEvent event);

    @Override
    List<SocialEvent> recent(Instant since);
}
