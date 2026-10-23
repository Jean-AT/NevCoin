package com.trading.nevcoin.history.application;

import com.trading.nevcoin.history.application.ports.HistoricalReplayDataSource;
import com.trading.nevcoin.history.domain.ReplayRequest;
import com.trading.nevcoin.history.domain.ReplaySourceData;
import com.trading.nevcoin.market.domain.MarketTick;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoricalReplayServiceTest {
    private static final Instant BASE = Instant.parse("2026-10-22T00:00:00Z");

    @Test
    void excludesFutureMarketObservationsAndSortsByEventTime() {
        var source = (HistoricalReplayDataSource) request -> new ReplaySourceData(List.of(), List.of(), List.of(
                tick("MINT", BASE.plusSeconds(30)), tick("MINT", BASE.plusSeconds(10)),
                tick("MINT", BASE.plusSeconds(70))), List.of(), List.of(), List.of());
        var service = new HistoricalReplayService(source);

        var dataset = service.replay(new ReplayRequest(BASE, BASE.plusSeconds(120), BASE.plusSeconds(40), Set.of("MINT")));

        assertEquals(List.of(BASE.plusSeconds(10), BASE.plusSeconds(30)),
                dataset.marketTicks().stream().map(MarketTick::observedAt).toList());
    }

    @Test
    void emptyTokenFilterKeepsAllTokensAndReplayIsRepeatable() {
        var source = (HistoricalReplayDataSource) request -> new ReplaySourceData(List.of(), List.of(), List.of(
                tick("A", BASE.plusSeconds(1)), tick("B", BASE.plusSeconds(2))), List.of(), List.of(), List.of());
        var service = new HistoricalReplayService(source);
        var request = new ReplayRequest(BASE, BASE.plusSeconds(10), BASE.plusSeconds(10), Set.of());

        var first = service.replay(request);
        var second = service.replay(request);

        assertEquals(first, second);
        assertEquals(2, first.marketTicks().size());
    }

    private MarketTick tick(String token, Instant observedAt) {
        return new MarketTick(token, observedAt, observedAt, new BigDecimal("1.00"),
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, "TEST");
    }
}
