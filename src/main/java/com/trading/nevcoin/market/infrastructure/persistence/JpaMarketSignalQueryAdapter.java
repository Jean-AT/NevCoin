package com.trading.nevcoin.market.infrastructure.persistence;

import com.trading.nevcoin.market.application.ports.MarketSignalQueryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class JpaMarketSignalQueryAdapter implements MarketSignalQueryPort {

    private final MarketSignalJpaRepository signalRepository;
    private final MarketSnapshotJpaRepository snapshotRepository;

    public JpaMarketSignalQueryAdapter(
            MarketSignalJpaRepository signalRepository,
            MarketSnapshotJpaRepository snapshotRepository) {
        this.signalRepository = signalRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalSummary> activeSignals(Instant now) {
        return signalRepository.findByExpiresAtAfterOrderByObservedAtDesc(now).stream()
                .map(signal -> {
                    MarketSnapshotEntity snapshot = snapshotRepository
                            .findTopByTokenAddressOrderByTimestampDesc(signal.getTokenAddress())
                            .orElse(null);
                    return new SignalSummary(
                            signal.getTokenAddress(), signal.getType(), signal.getStrength(),
                            signal.getObservedAt(), signal.getExpiresAt(), signal.getEvidence(),
                            snapshot == null ? null : snapshot.getPriceUsd(),
                            snapshot == null ? null : snapshot.getLiquidityUsd(),
                            snapshot == null ? null : snapshot.getVolume24hUsd(),
                            snapshot == null ? null : snapshot.getNetFlow5m());
                })
                .toList();
    }
}
