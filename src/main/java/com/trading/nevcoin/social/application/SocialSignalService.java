package com.trading.nevcoin.social.application;

import com.trading.nevcoin.social.application.ports.SocialSignalStore;
import com.trading.nevcoin.social.domain.SocialConfidence;
import com.trading.nevcoin.social.domain.SocialEvent;
import com.trading.nevcoin.social.domain.SocialSignal;
import com.trading.nevcoin.social.domain.SocialSignalEvidence;
import com.trading.nevcoin.social.domain.SocialSignalType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SocialSignalService {
    private static final Duration FRESHNESS = Duration.ofMinutes(15);
    private final SocialSignalStore store;

    public SocialSignalService(SocialSignalStore store) {
        this.store = store;
    }

    /** Produces at most one deterministic signal for a token-associated social event. */
    public List<SocialSignal> observe(SocialEvent event, Instant detectedAt) {
        if (event.tokenAssociations().isEmpty() || event.publishedAt().isAfter(detectedAt)) {
            return List.of();
        }
        return event.tokenAssociations().stream()
                .filter(association -> association.tokenAddress() != null && !association.tokenAddress().isBlank())
                .map(association -> toSignal(event, association.confidence(), association.tokenAddress()))
                .filter(store::saveIfNew)
                .toList();
    }

    private SocialSignal toSignal(SocialEvent event, SocialConfidence associationConfidence, String tokenAddress) {
        String eventIdentity = event.provider() + "|" + event.sourceId() + "|" + event.externalEventId() + "|" + tokenAddress;
        String signalId = UUID.nameUUIDFromBytes(eventIdentity.getBytes(StandardCharsets.UTF_8)).toString();
        BigDecimal confidence = confidence(associationConfidence);
        List<SocialSignalEvidence> evidence = List.of(new SocialSignalEvidence(
                event.externalEventId(), event.sourceAccountId(), "SOCIAL_EVENT", event.canonicalUrl(), event.publishedAt()));
        return new SocialSignal(signalId, tokenAddress, SocialSignalType.DIRECT_TOKEN_MENTION,
                confidence, confidence, event.publishedAt(), event.publishedAt(), event.publishedAt().plus(FRESHNESS),
                1, 1, evidence, "social-signal-v1");
    }

    private BigDecimal confidence(SocialConfidence confidence) {
        if (confidence == SocialConfidence.HIGH) return new BigDecimal("0.80");
        if (confidence == SocialConfidence.MEDIUM) return new BigDecimal("0.50");
        return new BigDecimal("0.20");
    }
}
