package com.trading.nevcoin.history.infrastructure.persistence;

import com.trading.nevcoin.paper.validation.domain.PaperValidationRequest;
import com.trading.nevcoin.paper.validation.domain.ReplayPricePoint;
import com.trading.nevcoin.paper.validation.domain.ReplaySignal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperHistoricalReplayAdapterTest {
    private static final Instant BASE = Instant.parse("2026-10-22T00:00:00Z");

    @Test
    void convertsCanonicalReplayToPaperValidationContract() {
        var canonical = new com.trading.nevcoin.history.domain.ReplayDataset(
                BASE, BASE.plusSeconds(10), BASE.plusSeconds(10), List.of(), List.of(),
                List.of(new com.trading.nevcoin.market.domain.MarketTick("MINT", BASE.plusSeconds(1), BASE.plusSeconds(1),
                        BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, "test")),
                List.of(), List.of(new com.trading.nevcoin.social.domain.SocialSignal("signal", "MINT",
                        com.trading.nevcoin.social.domain.SocialSignalType.DIRECT_TOKEN_MENTION, BigDecimal.ONE,
                        BigDecimal.ONE, BASE, BASE, BASE.plusSeconds(10), 1, 1, List.of(), "social-v1")),
                List.of(), Set.of("social-v1"));

        var adapter = new PaperHistoricalReplayAdapter(new com.trading.nevcoin.history.application.HistoricalReplayService(
                request -> new com.trading.nevcoin.history.domain.ReplaySourceData(
                        canonical.rawSocialEvents(), canonical.socialEvents(), canonical.marketTicks(),
                        canonical.marketSnapshots(), canonical.socialSignals(), canonical.crossDomainSignals())));
        var request = new PaperValidationRequest(BASE, BASE.plusSeconds(10), BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ZERO, 0, 1, Set.of(), Set.of(), BigDecimal.ZERO, BigDecimal.ZERO);

        // The canonical adapter must expose canonical observations; this fixture also asserts
        // the conversion contract independently from any database provider.
        var result = adapter.replay(request);
        assertEquals(1, result.prices().size());
        assertEquals("MINT", result.prices().getFirst().tokenAddress());
        assertEquals(1, result.signals().size());
        assertEquals("MINT", result.signals().getFirst().tokenAddress());
    }
}
