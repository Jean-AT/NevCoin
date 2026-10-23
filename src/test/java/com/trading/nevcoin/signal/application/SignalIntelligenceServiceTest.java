package com.trading.nevcoin.signal.application;

import com.trading.nevcoin.market.domain.MarketSignal;
import com.trading.nevcoin.signal.application.ports.SignalStore;
import com.trading.nevcoin.signal.application.ports.WalletSignalEvent;
import com.trading.nevcoin.signal.domain.CrossDomainSignal;
import com.trading.nevcoin.signal.domain.SignalPriority;
import com.trading.nevcoin.social.domain.SocialSignal;
import com.trading.nevcoin.social.domain.SocialSignalEvidence;
import com.trading.nevcoin.social.domain.SocialSignalType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SignalIntelligenceServiceTest {
    private final Instant now = Instant.parse("2026-10-22T10:00:00Z");

    @Test void correlatedEvidenceProducesOneHighPriorityTraceableSignal() {
        MemoryStore store = new MemoryStore();
        SignalIntelligenceService service = service(store);
        service.observe(social(), now);
        service.observe(market(now.minusSeconds(15)), now);
        service.observe(wallet("one", now.minusSeconds(10)), now);
        service.observe(wallet("two", now.minusSeconds(9)), now);
        var results = service.observe(wallet("three", now.minusSeconds(8)), now);
        assertEquals(1, results.size());
        assertEquals(SignalPriority.HIGH, results.getFirst().priority());
        assertEquals(5, results.getFirst().evidence().size());
        assertTrue(service.observe(wallet("three", now.minusSeconds(8)), now).isEmpty()); // same evidence set is persisted once
        assertEquals(1, store.values.size());
    }

    @Test void futureOrOutOfWindowEvidenceCannotConfirmSignalAndWalletCanBeAbsent() {
        SignalIntelligenceService service = service(new MemoryStore());
        service.observe(social(), now);
        service.observe(market(now.plusSeconds(1)), now);
        assertTrue(service.observe(wallet("one", now.minusSeconds(5)), now).isEmpty());
        assertTrue(service.highPriority(now).isEmpty());
    }

    @Test void reprocessingSameInputsProducesSameIdentity() {
        String first = produce(service(new MemoryStore()));
        String second = produce(service(new MemoryStore()));
        assertEquals(first, second);
    }

    private String produce(SignalIntelligenceService service) {
        service.observe(social(), now); service.observe(market(now), now);
        service.observe(wallet("one", now), now); service.observe(wallet("two", now), now);
        return service.observe(wallet("three", now), now).getFirst().signalId();
    }

    private SignalIntelligenceService service(MemoryStore store) {
        SignalIntelligenceProperties properties = new SignalIntelligenceProperties();
        properties.setCorrelationWindow(Duration.ofMinutes(10)); properties.setExpiry(Duration.ofMinutes(15));
        return new SignalIntelligenceService(store, properties, new StaticListableBeanFactory().getBeanProvider(com.trading.nevcoin.signal.application.ports.SignalPublishedEventPublisher.class));
    }
    private SocialSignal social() { return new SocialSignal("social-1", "MINT", SocialSignalType.DIRECT_TOKEN_MENTION, BigDecimal.ONE, BigDecimal.ONE, now, now, now.plusSeconds(30), 1, 1, List.of(new SocialSignalEvidence("event", "acct", "SOCIAL", "url", now)), "v1"); }
    private MarketSignal market(Instant observedAt) { return new MarketSignal("MINT", MarketSignal.SignalType.VOLUME_SPIKE_DETECTED, BigDecimal.valueOf(5), observedAt, now.plusSeconds(30), List.of("5x"), "good"); }
    private WalletSignalEvent wallet(String id, Instant observedAt) { return new WalletSignalEvent("wallet-" + id, "MINT", "tx-" + id, BigDecimal.ONE, observedAt); }
    private static class MemoryStore implements SignalStore {
        final List<CrossDomainSignal> values = new ArrayList<>();
        public boolean saveIfNew(CrossDomainSignal signal) { if (values.stream().anyMatch(value -> value.signalId().equals(signal.signalId()))) return false; values.add(signal); return true; }
        public List<CrossDomainSignal> recent(Instant since) { return List.copyOf(values); }
        public List<CrossDomainSignal> activeForToken(String token, Instant asOf) { return values.stream().filter(value -> value.tokenAddress().equals(token) && value.isActiveAt(asOf)).toList(); }
        public List<CrossDomainSignal> activeWithPriority(SignalPriority priority, Instant asOf) { return values.stream().filter(value -> value.priority() == priority && value.isActiveAt(asOf)).toList(); }
    }
}
