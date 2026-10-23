package com.trading.nevcoin.signal.application.ports;

import com.trading.nevcoin.signal.domain.CrossDomainSignal;
import com.trading.nevcoin.signal.domain.SignalPriority;

import java.time.Instant;
import java.util.List;

public interface SignalStore {
    boolean saveIfNew(CrossDomainSignal signal);
    List<CrossDomainSignal> recent(Instant since);
    List<CrossDomainSignal> activeForToken(String tokenAddress, Instant asOf);
    List<CrossDomainSignal> activeWithPriority(SignalPriority priority, Instant asOf);
}
