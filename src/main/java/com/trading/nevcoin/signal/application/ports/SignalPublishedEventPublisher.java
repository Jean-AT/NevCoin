package com.trading.nevcoin.signal.application.ports;

import com.trading.nevcoin.signal.domain.CrossDomainSignal;

/** Boundary for durable notification/outbox delivery introduced later. */
public interface SignalPublishedEventPublisher {
    void publish(CrossDomainSignal signal);
}
