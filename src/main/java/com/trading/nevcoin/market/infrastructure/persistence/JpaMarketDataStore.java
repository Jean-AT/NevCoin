package com.trading.nevcoin.market.infrastructure.persistence;

import com.trading.nevcoin.market.application.ports.MarketDataStore;
import com.trading.nevcoin.market.domain.MarketSignal;
import com.trading.nevcoin.market.domain.MarketSnapshot;
import com.trading.nevcoin.market.domain.MarketTick;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class JpaMarketDataStore implements MarketDataStore {

    private final MarketTickJpaRepository tickRepository;
    private final MarketSnapshotJpaRepository snapshotRepository;
    private final MarketSignalJpaRepository signalRepository;

    public JpaMarketDataStore(
            MarketTickJpaRepository tickRepository,
            MarketSnapshotJpaRepository snapshotRepository,
            MarketSignalJpaRepository signalRepository) {
        this.tickRepository = tickRepository;
        this.snapshotRepository = snapshotRepository;
        this.signalRepository = signalRepository;
    }

    @Override
    @Transactional
    public void saveTick(MarketTick tick) {
        tickRepository.save(new MarketTickEntity(tick));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketTick> recentTicks(String tokenAddress, int limit) {
        return tickRepository.findTop32ByTokenAddressOrderByObservedAtDesc(tokenAddress)
                .stream()
                .limit(limit)
                .map(MarketTickEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void saveSnapshot(MarketSnapshot snapshot) {
        snapshotRepository.save(new MarketSnapshotEntity(snapshot));
    }

    @Override
    @Transactional
    public void saveSignal(MarketSignal signal) {
        signalRepository.save(new MarketSignalEntity(signal));
    }
}
