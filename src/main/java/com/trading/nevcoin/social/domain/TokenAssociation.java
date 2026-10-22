package com.trading.nevcoin.social.domain;

import java.util.List;

public record TokenAssociation(
        String tokenAddress,
        String associationType,
        SocialConfidence confidence,
        String extractorVersion,
        List<SocialEvidence> evidence) {
    public TokenAssociation {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
