package com.trading.nevcoin.social.domain;

import java.time.Instant;

/** Traceable input used to produce a descriptive social signal. */
public record SocialSignalEvidence(
        String sourceEventId,
        String sourceAccountId,
        String evidenceType,
        String value,
        Instant observedAt) {
}
