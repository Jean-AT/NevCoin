package com.trading.nevcoin.history.application;

import com.trading.nevcoin.history.application.ports.HistoricalReplayDataSource;
import com.trading.nevcoin.history.application.ports.HistoricalReplayPort;
import com.trading.nevcoin.history.domain.ReplayDataset;
import com.trading.nevcoin.history.domain.ReplayRequest;
import com.trading.nevcoin.history.domain.ReplaySourceData;
import com.trading.nevcoin.market.domain.MarketSnapshot;
import com.trading.nevcoin.market.domain.MarketTick;
import com.trading.nevcoin.signal.domain.CrossDomainSignal;
import com.trading.nevcoin.social.domain.RawSocialEvent;
import com.trading.nevcoin.social.domain.SocialEvent;
import com.trading.nevcoin.social.domain.SocialSignal;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

/** Builds deterministic, as-of views without consulting future observations. */
@Service
public class HistoricalReplayService implements HistoricalReplayPort {
    private final HistoricalReplayDataSource dataSource;

    public HistoricalReplayService(HistoricalReplayDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ReplayDataset replay(ReplayRequest request) {
        ReplaySourceData source = dataSource.load(request);
        Predicate<String> tokenFilter = token -> request.tokenAddresses().isEmpty()
                || (token != null && request.tokenAddresses().contains(token));
        List<RawSocialEvent> rawEvents = source.rawSocialEvents().stream()
                .filter(event -> inRange(event.publishedAt() == null ? event.receivedAt() : event.publishedAt(), request))
                .filter(event -> event.receivedAt().compareTo(request.asOf()) <= 0)
                .sorted(Comparator.comparing(event -> event.publishedAt() == null ? event.receivedAt() : event.publishedAt()))
                .toList();
        List<SocialEvent> socialEvents = source.socialEvents().stream()
                .filter(event -> inRange(event.publishedAt(), request))
                .filter(event -> event.receivedAt().compareTo(request.asOf()) <= 0)
                .filter(event -> event.tokenAddress() == null || tokenFilter.test(event.tokenAddress()))
                .sorted(Comparator.comparing(SocialEvent::publishedAt))
                .toList();
        List<MarketTick> ticks = source.marketTicks().stream()
                .filter(tick -> inRange(tick.observedAt(), request) && tick.observedAt().compareTo(request.asOf()) <= 0)
                .filter(tick -> tokenFilter.test(tick.tokenAddress()))
                .sorted(Comparator.comparing(MarketTick::observedAt))
                .toList();
        List<MarketSnapshot> snapshots = source.marketSnapshots().stream()
                .filter(snapshot -> inRange(snapshot.timestamp(), request) && snapshot.timestamp().compareTo(request.asOf()) <= 0)
                .filter(snapshot -> tokenFilter.test(snapshot.tokenAddress()))
                .sorted(Comparator.comparing(MarketSnapshot::timestamp))
                .toList();
        List<SocialSignal> socialSignals = source.socialSignals().stream()
                .filter(signal -> inRange(signal.firstObservedAt(), request) && signal.firstObservedAt().compareTo(request.asOf()) <= 0)
                .filter(signal -> tokenFilter.test(signal.tokenAddress()))
                .sorted(Comparator.comparing(SocialSignal::firstObservedAt))
                .toList();
        List<CrossDomainSignal> crossSignals = source.crossDomainSignals().stream()
                .filter(signal -> inRange(signal.detectedAt(), request) && signal.detectedAt().compareTo(request.asOf()) <= 0)
                .filter(signal -> tokenFilter.test(signal.tokenAddress()))
                .sorted(Comparator.comparing(CrossDomainSignal::detectedAt))
                .toList();
        Set<String> versions = new HashSet<>();
        socialEvents.stream().map(SocialEvent::algorithmVersion).forEach(versions::add);
        socialSignals.stream().map(SocialSignal::algorithmVersion).forEach(versions::add);
        crossSignals.stream().map(CrossDomainSignal::algorithmVersion).forEach(versions::add);
        return new ReplayDataset(request.from(), request.to(), request.asOf(), rawEvents, socialEvents, ticks,
                snapshots, socialSignals, crossSignals, versions);
    }

    private boolean inRange(Instant value, ReplayRequest request) {
        return value != null && !value.isBefore(request.from()) && !value.isAfter(request.to());
    }
}
