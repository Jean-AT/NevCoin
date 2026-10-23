package com.trading.nevcoin.social.application.ports;

import com.trading.nevcoin.social.domain.SocialSignal;

import java.time.Instant;
import java.util.List;

public interface SocialSignalStore {
    boolean saveIfNew(SocialSignal signal);
    List<SocialSignal> recent(Instant since);
    List<SocialSignal> activeForToken(String tokenAddress, Instant asOf);
}
