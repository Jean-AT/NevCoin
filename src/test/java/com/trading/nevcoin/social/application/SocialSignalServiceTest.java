package com.trading.nevcoin.social.application;

import com.trading.nevcoin.social.application.ports.SocialSignalStore;
import com.trading.nevcoin.social.domain.SocialConfidence;
import com.trading.nevcoin.social.domain.SocialEvent;
import com.trading.nevcoin.social.domain.SocialEventStatus;
import com.trading.nevcoin.social.domain.SocialEventType;
import com.trading.nevcoin.social.domain.TokenAssociation;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SocialSignalServiceTest {
    private final Instant now = Instant.parse("2026-10-22T10:00:00Z");

    @Test void directMentionCreatesTraceableSignalAndDuplicateDoesNot() {
        MemoryStore store = new MemoryStore();
        SocialSignalService service = new SocialSignalService(store);
        SocialEvent event = event("event-1", "MINT", SocialConfidence.HIGH, now.minusSeconds(20));

        var signal = service.observe(event, now);

        assertEquals(1, signal.size());
        assertEquals("MINT", signal.getFirst().tokenAddress());
        assertEquals("event-1", signal.getFirst().evidence().getFirst().sourceEventId());
        assertTrue(service.observe(event, now).isEmpty());
    }

    @Test void noTokenAndFutureEventDoNotBecomeTokenSignals() {
        SocialSignalService service = new SocialSignalService(new MemoryStore());
        assertTrue(service.observe(event("none", null, SocialConfidence.MEDIUM, now.minusSeconds(1)), now).isEmpty());
        assertTrue(service.observe(event("future", "MINT", SocialConfidence.HIGH, now.plusSeconds(1)), now).isEmpty());
    }

    @Test void ambiguousAssociationKeepsLowConfidenceAndSignalExpires() {
        SocialSignalService service = new SocialSignalService(new MemoryStore());
        var signal = service.observe(event("ambiguous", "MINT", SocialConfidence.LOW, now), now).getFirst();
        assertEquals(0, signal.confidence().compareTo(new java.math.BigDecimal("0.20")));
        assertFalse(signal.isActiveAt(signal.expiresAt().plusMillis(1)));
    }

    private SocialEvent event(String id, String token, SocialConfidence confidence, Instant observedAt) {
        return new SocialEvent("rss", "source", "account", id, "https://example.test/" + id, observedAt, observedAt,
                observedAt, null, SocialEventType.TOKEN_MENTION, "mention", id, SocialEventStatus.ASSOCIATED, "v1", List.of(),
                List.of(), token == null ? List.of() : List.of(new TokenAssociation(token, "DIRECT", confidence, "v1", List.of())), null, confidence);
    }

    private static class MemoryStore implements SocialSignalStore {
        private final List<com.trading.nevcoin.social.domain.SocialSignal> values = new ArrayList<>();
        public boolean saveIfNew(com.trading.nevcoin.social.domain.SocialSignal signal) { if (values.stream().anyMatch(value -> value.signalId().equals(signal.signalId()))) return false; values.add(signal); return true; }
        public List<com.trading.nevcoin.social.domain.SocialSignal> recent(Instant since) { return List.copyOf(values); }
        public List<com.trading.nevcoin.social.domain.SocialSignal> activeForToken(String token, Instant asOf) { return values.stream().filter(value -> value.tokenAddress().equals(token) && value.isActiveAt(asOf)).toList(); }
    }
}
