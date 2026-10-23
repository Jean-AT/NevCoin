package com.trading.nevcoin.paper.validation.application;

import com.trading.nevcoin.paper.validation.application.ports.HistoricalReplayPort;
import com.trading.nevcoin.paper.validation.domain.PaperValidationRequest;
import com.trading.nevcoin.paper.validation.domain.PaperValidationResult;
import com.trading.nevcoin.paper.validation.domain.ReplayDataset;
import com.trading.nevcoin.paper.validation.domain.ReplayPricePoint;
import com.trading.nevcoin.paper.validation.domain.ReplaySignal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PaperValidationServiceTest {
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test void entryUsesFirstPriceStrictlyAfterDetectionAndNeverEarlierFutureLookingPrice() {
        ReplaySignal signal = signal("s1", "TOKEN", T0.plusSeconds(60), T0.plusSeconds(600));
        ReplayDataset data = new ReplayDataset(List.of(signal), List.of(
                price("TOKEN", T0.plusSeconds(30), "1000"),
                price("TOKEN", T0.plusSeconds(120), "100")), Set.of("social-v1"));
        PaperValidationResult result = service(data).validate(request(T0, T0.plusSeconds(300), "1000", "100", 0, 0, 5, 0, 0), data);
        assertEquals(1, result.tradeResults().size());
        assertEquals(0, result.tradeResults().getFirst().entryPrice().compareTo(new BigDecimal("100")));
    }

    @Test void feesAndDirectionalSlippageReduceReturn() {
        ReplaySignal signal = signal("s1", "TOKEN", T0, T0.plusSeconds(600));
        ReplayDataset data = new ReplayDataset(List.of(signal), List.of(price("TOKEN", T0.plusSeconds(1), "100"), price("TOKEN", T0.plusSeconds(2), "110")), Set.of());
        PaperValidationResult result = service(data).validate(request(T0, T0.plusSeconds(2), "1000", "100", 1, 100, 5, 5, 0), data);
        var trade = result.tradeResults().getFirst();
        assertTrue(trade.entryPrice().compareTo(new BigDecimal("100")) > 0, trade.toString());
        assertTrue(trade.exitPrice().compareTo(new BigDecimal("110")) == 0);
        assertTrue(trade.feesUsd().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(trade.slippageUsd().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(trade.realizedPnlUsd().compareTo(new BigDecimal("10")) < 0);
    }

    @Test void takeProfitAndStopLossClosePositionsWithExplicitReasons() {
        ReplaySignal takeProfit = signal("tp", "A", T0, T0.plusSeconds(600));
        ReplaySignal stopLoss = signal("sl", "B", T0, T0.plusSeconds(600));
        ReplayDataset data = new ReplayDataset(List.of(takeProfit, stopLoss), List.of(
                price("A", T0.plusSeconds(1), "100"), price("A", T0.plusSeconds(2), "106"),
                price("B", T0.plusSeconds(1), "100"), price("B", T0.plusSeconds(2), "94")), Set.of());
        PaperValidationResult result = service(data).validate(request(T0, T0.plusSeconds(2), "1000", "100", 0, 0, 5, 5, 5), data);
        assertTrue(result.tradeResults().stream().anyMatch(trade -> trade.exitReason().equals("TAKE_PROFIT")));
        assertTrue(result.tradeResults().stream().anyMatch(trade -> trade.exitReason().equals("STOP_LOSS")));
    }

    @Test void expiredSignalDoesNotEnterAndOpenPositionHasUnrealizedPnl() {
        ReplaySignal expired = signal("expired", "A", T0, T0.plusSeconds(2));
        ReplaySignal open = signal("open", "B", T0, T0.plusSeconds(600));
        ReplayDataset data = new ReplayDataset(List.of(expired, open), List.of(
                price("A", T0.plusSeconds(3), "100"), price("B", T0.plusSeconds(1), "100"), price("B", T0.plusSeconds(2), "110")), Set.of());
        PaperValidationResult result = service(data).validate(request(T0, T0.plusSeconds(10), "1000", "100", 0, 0, 5, 0, 0), data);
        assertEquals(1, result.tradeResults().size());
        assertTrue(result.tradeResults().getFirst().isOpen());
        assertTrue(result.unrealizedPnlUsd().compareTo(BigDecimal.ZERO) > 0, result.toString());
    }

    @Test void drawdownDuplicateSignalsPositionLimitAndCashLimitAreEnforced() {
        ReplaySignal first = signal("a", "A", T0, T0.plusSeconds(600));
        ReplaySignal duplicate = signal("a", "A", T0, T0.plusSeconds(600));
        ReplaySignal second = signal("b", "B", T0, T0.plusSeconds(600));
        ReplayDataset data = new ReplayDataset(List.of(first, duplicate, second), List.of(
                price("A", T0.plusSeconds(1), "100"), price("A", T0.plusSeconds(2), "90"),
                price("B", T0.plusSeconds(1), "100")), Set.of());
        PaperValidationResult result = service(data).validate(request(T0, T0.plusSeconds(10), "100", "100", 0, 0, 1, 0, 0), data);
        assertEquals(1, result.tradeResults().size());
        assertTrue(result.maxDrawdownUsd().compareTo(BigDecimal.ZERO) > 0);
        PaperValidationResult noCash = service(new ReplayDataset(List.of(first), List.of(price("A", T0.plusSeconds(1), "100")), Set.of()))
                .validate(request(T0, T0.plusSeconds(10), "50", "100", 0, 0, 5, 0, 0), data);
        assertTrue(noCash.tradeResults().isEmpty());
    }

    @Test void replayIsReproducibleAndApplicationPortIsUsed() {
        ReplaySignal signal = signal("s", "A", T0, T0.plusSeconds(600));
        ReplayDataset data = new ReplayDataset(List.of(signal), List.of(price("A", T0.plusSeconds(1), "100"), price("A", T0.plusSeconds(2), "110")), Set.of("v1"));
        HistoricalReplayPort port = ignored -> data;
        PaperValidationRequest request = request(T0, T0.plusSeconds(2), "1000", "100", 0, 0, 5, 5, 0);
        PaperValidationService service = new PaperValidationService(port);
        assertEquals(service.validate(request), service.validate(request));
        assertTrue(service.validate(request).algorithmVersions().contains("v1"));
    }

    private PaperValidationService service(ReplayDataset ignored) { return new PaperValidationService((HistoricalReplayPort) request -> ignored); }
    private static ReplaySignal signal(String id, String token, Instant detectedAt, Instant expiresAt) { return new ReplaySignal(id, token, "MOMENTUM_DETECTED", detectedAt, expiresAt, List.of("evidence:" + id), "v1"); }
    private static ReplayPricePoint price(String token, Instant at, String value) { return new ReplayPricePoint(token, at, new BigDecimal(value), "fixture"); }
    private static PaperValidationRequest request(Instant from, Instant to, String cash, String notional, int fee, int slippage, int maxPositions, int tp, int sl) {
        return new PaperValidationRequest(from, to, new BigDecimal(cash), new BigDecimal(notional), BigDecimal.valueOf(fee), slippage, maxPositions, Set.of(), Set.of(), BigDecimal.valueOf(tp), BigDecimal.valueOf(sl));
    }
}
