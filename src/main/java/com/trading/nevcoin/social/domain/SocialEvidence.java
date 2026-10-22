package com.trading.nevcoin.social.domain;

import java.time.Instant;

public record SocialEvidence(
        String type,
        String value,
        String sourceEventId,
        Instant observedAt,
        SocialConfidence confidence) {
}
