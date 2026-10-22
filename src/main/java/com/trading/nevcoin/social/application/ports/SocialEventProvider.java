package com.trading.nevcoin.social.application.ports;

import com.trading.nevcoin.social.domain.RawSocialEvent;

public interface SocialEventProvider {
    SocialFetchResult fetch(String sourceId, String sourceUrl, String cursor, int limit);
}
