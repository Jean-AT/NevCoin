package com.trading.nevcoin.history.infrastructure.persistence;

import com.trading.nevcoin.history.application.ports.HistoricalReplayDataSource;
import com.trading.nevcoin.history.domain.ReplayRequest;
import com.trading.nevcoin.history.domain.ReplaySourceData;
import com.trading.nevcoin.market.infrastructure.persistence.MarketSnapshotEntity;
import com.trading.nevcoin.market.infrastructure.persistence.MarketSnapshotJpaRepository;
import com.trading.nevcoin.market.infrastructure.persistence.MarketTickJpaRepository;
import com.trading.nevcoin.signal.infrastructure.persistence.CrossDomainSignalJpaRepository;
import com.trading.nevcoin.social.infrastructure.persistence.SocialEventEntity;
import com.trading.nevcoin.social.infrastructure.persistence.SocialEventJpaRepository;
import com.trading.nevcoin.social.infrastructure.persistence.SocialRawEventJpaRepository;
import com.trading.nevcoin.social.infrastructure.persistence.SocialSignalJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the persisted observations available at an as-of boundary.
 * Domain replay remains responsible for the final event-time window and token filtering.
 */
@Component
public class JpaHistoricalReplayDataSource implements HistoricalReplayDataSource {
    private final SocialRawEventJpaRepository rawEvents;
    private final SocialEventJpaRepository socialEvents;
    private final MarketTickJpaRepository marketTicks;
    private final MarketSnapshotJpaRepository marketSnapshots;
    private final SocialSignalJpaRepository socialSignals;
    private final CrossDomainSignalJpaRepository crossDomainSignals;

    public JpaHistoricalReplayDataSource(
            SocialRawEventJpaRepository rawEvents,
            SocialEventJpaRepository socialEvents,
            MarketTickJpaRepository marketTicks,
            MarketSnapshotJpaRepository marketSnapshots,
            SocialSignalJpaRepository socialSignals,
            CrossDomainSignalJpaRepository crossDomainSignals) {
        this.rawEvents = rawEvents;
        this.socialEvents = socialEvents;
        this.marketTicks = marketTicks;
        this.marketSnapshots = marketSnapshots;
        this.socialSignals = socialSignals;
        this.crossDomainSignals = crossDomainSignals;
    }

    @Override
    @Transactional(readOnly = true)
    public ReplaySourceData load(ReplayRequest request) {
        return new ReplaySourceData(
                rawEvents.findByReceivedAtLessThanEqualOrderByReceivedAtAsc(request.asOf()).stream()
                        .map(entity -> entity.toDomain()).toList(),
                socialEvents.findByReceivedAtLessThanEqualOrderByReceivedAtAsc(request.asOf()).stream()
                        .map(SocialEventEntity::toDomain).toList(),
                marketTicks.findByObservedAtLessThanEqualOrderByObservedAtAsc(request.asOf()).stream()
                        .map(entity -> entity.toDomain()).toList(),
                marketSnapshots.findByTimestampLessThanEqualOrderByTimestampAsc(request.asOf()).stream()
                        .map(MarketSnapshotEntity::toDomain).toList(),
                socialSignals.findByFirstObservedAtLessThanEqualOrderByFirstObservedAtAsc(request.asOf()).stream()
                        .map(entity -> entity.toDomain()).toList(),
                crossDomainSignals.findByDetectedAtLessThanEqualOrderByDetectedAtAsc(request.asOf()).stream()
                        .map(entity -> entity.toDomain()).toList());
    }
}
