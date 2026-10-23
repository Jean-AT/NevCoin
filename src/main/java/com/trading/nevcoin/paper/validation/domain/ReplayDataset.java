package com.trading.nevcoin.paper.validation.domain;

import java.util.List;
import java.util.Set;

public record ReplayDataset(List<ReplaySignal> signals, List<ReplayPricePoint> prices, Set<String> algorithmVersions) {
    public ReplayDataset {
        signals = signals == null ? List.of() : List.copyOf(signals);
        prices = prices == null ? List.of() : List.copyOf(prices);
        algorithmVersions = algorithmVersions == null ? Set.of() : Set.copyOf(algorithmVersions);
    }
}
