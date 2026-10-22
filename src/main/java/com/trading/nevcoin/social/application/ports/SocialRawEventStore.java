package com.trading.nevcoin.social.application.ports;

import com.trading.nevcoin.social.domain.RawSocialEvent;

public interface SocialRawEventStore {
    boolean saveIfNew(RawSocialEvent event);
}
