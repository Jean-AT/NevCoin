package com.trading.nevcoin.social.application.ports;

import com.trading.nevcoin.social.domain.SocialEvent;

import java.time.Instant;
import java.util.List;

public interface SocialEventQueryPort {
    List<SocialEvent> recent(Instant since);
}
