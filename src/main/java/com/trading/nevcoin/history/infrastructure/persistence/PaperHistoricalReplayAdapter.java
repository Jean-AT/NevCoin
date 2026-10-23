package com.trading.nevcoin.history.infrastructure.persistence;

import com.trading.nevcoin.history.application.HistoricalReplayService;
import com.trading.nevcoin.history.domain.ReplayDataset;
import com.trading.nevcoin.history.domain.ReplayRequest;
import com.trading.nevcoin.paper.validation.domain.PaperValidationRequest;
import com.trading.nevcoin.paper.validation.domain.ReplayPricePoint;
import com.trading.nevcoin.paper.validation.domain.ReplaySignal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/** Bridges the rich historical replay model to the paper-validation input contract. */
@Component
public class PaperHistoricalReplayAdapter implements com.trading.nevcoin.paper.validation.application.ports.HistoricalReplayPort {
    private final HistoricalReplayService replayService;

    public PaperHistoricalReplayAdapter(HistoricalReplayService replayService) {
        this.replayService = replayService;
    }

    @Override
    public com.trading.nevcoin.paper.validation.domain.ReplayDataset replay(PaperValidationRequest request) {
        ReplayDataset dataset = replayService.replay(new ReplayRequest(
                request.from(), request.to(), request.to(), request.tokenAddresses()));
        var signals = new ArrayList<ReplaySignal>();
        dataset.socialSignals().forEach(signal -> signals.add(new ReplaySignal(
                signal.signalId(), signal.tokenAddress(), signal.signalType().name(), signal.firstObservedAt(),
                signal.expiresAt(), signal.evidence().stream().map(evidence -> evidence.value()).toList(),
                signal.algorithmVersion())));
        dataset.crossDomainSignals().forEach(signal -> signals.add(new ReplaySignal(
                signal.signalId(), signal.tokenAddress(), "CROSS_DOMAIN", signal.detectedAt(), signal.expiresAt(),
                signal.evidence().stream().map(evidence -> evidence.description()).toList(), signal.algorithmVersion())));
        var prices = new ArrayList<ReplayPricePoint>();
        dataset.marketTicks().forEach(tick -> {
            if (tick.priceUsd() != null) prices.add(new ReplayPricePoint(tick.tokenAddress(), tick.observedAt(), tick.priceUsd(), tick.sourceQuality()));
        });
        dataset.marketSnapshots().forEach(snapshot -> {
            if (snapshot.priceUsd() != null) prices.add(new ReplayPricePoint(snapshot.tokenAddress(), snapshot.timestamp(), snapshot.priceUsd(), "market-snapshot"));
        });
        return new com.trading.nevcoin.paper.validation.domain.ReplayDataset(signals, prices, dataset.algorithmVersions());
    }
}
