package com.trading.nevcoin.signal.application;

import com.trading.nevcoin.market.domain.MarketSignal;
import com.trading.nevcoin.signal.application.ports.SignalPublishedEventPublisher;
import com.trading.nevcoin.signal.application.ports.SignalStore;
import com.trading.nevcoin.signal.application.ports.WalletSignalEvent;
import com.trading.nevcoin.signal.domain.CrossDomainSignal;
import com.trading.nevcoin.signal.domain.SignalComponent;
import com.trading.nevcoin.signal.domain.SignalEvidence;
import com.trading.nevcoin.signal.domain.SignalPriority;
import com.trading.nevcoin.social.domain.SocialSignal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SignalIntelligenceService {
    private final SignalStore store;
    private final SignalIntelligenceProperties properties;
    private final ObjectProvider<SignalPublishedEventPublisher> publisher;
    private final Map<String, List<Observed>> observations = new HashMap<>();

    public SignalIntelligenceService(SignalStore store, SignalIntelligenceProperties properties,
                                     ObjectProvider<SignalPublishedEventPublisher> publisher) {
        this.store = store;
        this.properties = properties;
        this.publisher = publisher;
    }

    public synchronized List<CrossDomainSignal> observe(SocialSignal signal, Instant detectedAt) {
        return observe(new Observed(SignalComponent.SOCIAL, signal.tokenAddress(), signal.signalId(),
                "Social " + signal.signalType(), signal.lastObservedAt(), signal.confidence()), detectedAt);
    }

    public synchronized List<CrossDomainSignal> observe(MarketSignal signal, Instant detectedAt) {
        return observe(new Observed(SignalComponent.MARKET, signal.tokenAddress(),
                signal.type() + ":" + signal.observedAt(), "Market " + signal.type(), signal.observedAt(), signal.strength()), detectedAt);
    }

    public synchronized List<CrossDomainSignal> observe(WalletSignalEvent event, Instant detectedAt) {
        return observe(new Observed(SignalComponent.WALLET, event.tokenAddress(), event.transactionSignature(),
                "Wallet purchase " + event.walletAddress(), event.observedAt(), BigDecimal.ONE), detectedAt);
    }

    private List<CrossDomainSignal> observe(Observed observed, Instant detectedAt) {
        if (observed.tokenAddress == null || observed.tokenAddress.isBlank() || observed.observedAt.isAfter(detectedAt)) {
            return List.of();
        }
        List<Observed> tokenObservations = observations.computeIfAbsent(observed.tokenAddress, ignored -> new ArrayList<>());
        tokenObservations.removeIf(candidate -> candidate.observedAt.isBefore(detectedAt.minus(properties.getCorrelationWindow())));
        if (tokenObservations.stream().noneMatch(candidate -> candidate.component == observed.component && candidate.id.equals(observed.id))) {
            tokenObservations.add(observed);
        }
        return createCrossSignal(observed.tokenAddress, tokenObservations, detectedAt);
    }

    private List<CrossDomainSignal> createCrossSignal(String tokenAddress, List<Observed> candidates, Instant detectedAt) {
        List<Observed> eligible = candidates.stream()
                .filter(candidate -> !candidate.observedAt.isAfter(detectedAt))
                .filter(candidate -> !candidate.observedAt.isBefore(detectedAt.minus(properties.getCorrelationWindow())))
                .toList();
        Set<SignalComponent> components = eligible.stream().map(candidate -> candidate.component).collect(java.util.stream.Collectors.toSet());
        long wallets = eligible.stream().filter(candidate -> candidate.component == SignalComponent.WALLET)
                .map(candidate -> candidate.id).distinct().count();
        if (!components.containsAll(Set.of(SignalComponent.SOCIAL, SignalComponent.MARKET, SignalComponent.WALLET)) || wallets < 3) {
            return List.of();
        }
        List<Observed> inputs = eligible.stream().sorted(Comparator.comparing(Observed::id)).toList();
        String identity = tokenAddress + "|" + inputs.stream().map(Observed::id).reduce("", (left, right) -> left + "|" + right);
        CrossDomainSignal signal = new CrossDomainSignal(
                UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString(), tokenAddress, SignalPriority.HIGH,
                BigDecimal.valueOf(inputs.size()), confidence(inputs), detectedAt, detectedAt.plus(properties.getExpiry()), components,
                inputs.stream().map(input -> new SignalEvidence(input.component, input.id, input.description, input.observedAt)).toList(),
                "cross-signal-v1");
        if (!store.saveIfNew(signal)) return List.of();
        SignalPublishedEventPublisher eventPublisher = publisher.getIfAvailable();
        if (eventPublisher != null) eventPublisher.publish(signal);
        return List.of(signal);
    }

    private BigDecimal confidence(List<Observed> inputs) {
        return inputs.stream().map(Observed::confidence).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(inputs.size()), 4, java.math.RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
    }

    public List<CrossDomainSignal> recent(Instant since) { return store.recent(since); }
    public List<CrossDomainSignal> byToken(String tokenAddress, Instant asOf) { return store.activeForToken(tokenAddress, asOf); }
    public List<CrossDomainSignal> highPriority(Instant asOf) { return store.activeWithPriority(SignalPriority.HIGH, asOf); }

    private record Observed(SignalComponent component, String tokenAddress, String id, String description,
                            Instant observedAt, BigDecimal confidence) { }
}
