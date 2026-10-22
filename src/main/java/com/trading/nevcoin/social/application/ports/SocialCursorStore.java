package com.trading.nevcoin.social.application.ports;

import java.time.Instant;
import java.util.Optional;

public interface SocialCursorStore {
    Optional<String> current(String sourceId);
    void advance(String sourceId, String cursor, Instant fetchedAt);
}
