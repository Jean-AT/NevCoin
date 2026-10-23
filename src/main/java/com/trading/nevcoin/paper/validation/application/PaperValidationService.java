package com.trading.nevcoin.paper.validation.application;

import com.trading.nevcoin.paper.validation.application.ports.HistoricalReplayPort;
import com.trading.nevcoin.paper.validation.domain.EquityPoint;
import com.trading.nevcoin.paper.validation.domain.PaperValidationRequest;
import com.trading.nevcoin.paper.validation.domain.PaperValidationResult;
import com.trading.nevcoin.paper.validation.domain.PaperValidationTrade;
import com.trading.nevcoin.paper.validation.domain.ReplayDataset;
import com.trading.nevcoin.paper.validation.domain.ReplayPricePoint;
import com.trading.nevcoin.paper.validation.domain.ReplaySignal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Pure, event-time paper validation. It does not place orders or call market providers. */
@Service
public class PaperValidationService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 12;
    private final ObjectProvider<HistoricalReplayPort> replayPort;

    @Autowired
    public PaperValidationService(ObjectProvider<HistoricalReplayPort> replayPort) {
        this.replayPort = replayPort;
    }

    public PaperValidationService(HistoricalReplayPort replayPort) {
        this.replayPort = new ObjectProvider<>() {
            @Override public HistoricalReplayPort getObject(Object... args) { return replayPort; }
            @Override public HistoricalReplayPort getIfAvailable() { return replayPort; }
            @Override public HistoricalReplayPort getIfUnique() { return replayPort; }
            @Override public HistoricalReplayPort getIfAvailable(java.util.function.Supplier<HistoricalReplayPort> supplier) { return replayPort; }
            @Override public HistoricalReplayPort getIfUnique(java.util.function.Supplier<HistoricalReplayPort> supplier) { return replayPort; }
            @Override public java.util.Iterator<HistoricalReplayPort> iterator() { return java.util.List.of(replayPort).iterator(); }
        };
    }

    public PaperValidationResult validate(PaperValidationRequest request) {
        HistoricalReplayPort port = replayPort.getIfAvailable();
        if (port == null) throw new IllegalStateException("HistoricalReplayPort is not configured");
        return validate(request, port.replay(request));
    }

    public PaperValidationResult validate(PaperValidationRequest request, ReplayDataset dataset) {
        List<ReplaySignal> signals = dataset.signals().stream()
                .filter(signal -> inPeriod(signal.detectedAt(), request))
                .filter(signal -> request.signalTypes().isEmpty() || request.signalTypes().contains(signal.signalType()))
                .filter(signal -> request.tokenAddresses().isEmpty() || request.tokenAddresses().contains(signal.tokenAddress()))
                .sorted(Comparator.comparing(ReplaySignal::detectedAt).thenComparing(ReplaySignal::signalId))
                .collect(Collectors.collectingAndThen(Collectors.toMap(ReplaySignal::signalId, signal -> signal,
                        (first, ignored) -> first, LinkedHashMap::new), map -> new ArrayList<>(map.values())));
        List<ReplayPricePoint> prices = dataset.prices().stream()
                .filter(price -> inPeriod(price.observedAt(), request))
                .sorted(Comparator.comparing(ReplayPricePoint::observedAt)
                        .thenComparing(ReplayPricePoint::tokenAddress)
                        .thenComparing(ReplayPricePoint::sourceId))
                .toList();

        Set<String> versions = new HashSet<>(dataset.algorithmVersions());
        signals.forEach(signal -> versions.add(signal.algorithmVersion()));
        Simulation simulation = new Simulation(request, signals, prices, versions);
        return simulation.run();
    }

    public PaperValidationTrade validateSignal(ReplaySignal signal, List<ReplayPricePoint> prices, PaperValidationRequest request) {
        ReplayDataset dataset = new ReplayDataset(List.of(signal), prices, Set.of(signal.algorithmVersion()));
        PaperValidationResult result = validate(request, dataset);
        return result.tradeResults().stream().findFirst().orElse(null);
    }

    private static boolean inPeriod(Instant time, PaperValidationRequest request) {
        return !time.isBefore(request.from()) && !time.isAfter(request.to());
    }

    private static final class Simulation {
        private final PaperValidationRequest request;
        private final List<ReplaySignal> signals;
        private final List<ReplayPricePoint> prices;
        private final Set<String> algorithmVersions;
        private final List<Position> open = new ArrayList<>();
        private final List<PaperValidationTrade> trades = new ArrayList<>();
        private final List<EquityPoint> equityCurve = new ArrayList<>();
        private final Set<String> attemptedSignals = new HashSet<>();
        private final Map<String, BigDecimal> returnByType = new HashMap<>();
        private final Map<String, BigDecimal> returnByToken = new HashMap<>();
        private BigDecimal cash;
        private BigDecimal peak = BigDecimal.ZERO;
        private BigDecimal maxDrawdown = BigDecimal.ZERO;
        private BigDecimal exposureTotal = BigDecimal.ZERO;
        private int exposurePoints;
        private BigDecimal holdingMinutesTotal = BigDecimal.ZERO;
        private int closedPositions;

        private Simulation(PaperValidationRequest request, List<ReplaySignal> signals, List<ReplayPricePoint> prices, Set<String> algorithmVersions) {
            this.request = request; this.signals = signals; this.prices = prices; this.algorithmVersions = algorithmVersions;
            this.cash = request.initialCashUsd();
        }

        private PaperValidationResult run() {
            Set<String> entered = new HashSet<>();
            for (ReplayPricePoint price : prices) {
                closePositions(price);
                for (ReplaySignal signal : signals) {
                    if (attemptedSignals.contains(signal.signalId()) || entered.contains(signal.signalId())
                            || !signal.tokenAddress().equals(price.tokenAddress()) || !signal.detectedAt().isBefore(price.observedAt())) continue;
                    attemptedSignals.add(signal.signalId());
                    if (isExpired(signal, price.observedAt()) || open.size() >= request.maxOpenPositions() || cash.compareTo(request.tradeNotionalUsd()) < 0) continue;
                    Position position = simulateEntry(signal, price);
                    open.add(position); entered.add(signal.signalId()); cash = cash.subtract(position.entryNotional);
                }
                recordEquity(price);
            }
            ReplayPricePoint last = prices.isEmpty() ? null : prices.getLast();
            for (Position position : open) {
                PaperValidationTrade trade = position.openTrade();
                trades.add(trade);
            }
            BigDecimal finalEquity = last == null ? cash : equityAt(last);
            BigDecimal realized = trades.stream().map(PaperValidationTrade::realizedPnlUsd).filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal unrealized = open.stream().map(position -> unrealizedAt(position, last)).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalReturn = finalEquity.subtract(request.initialCashUsd());
            long wins = trades.stream().filter(trade -> trade.realizedPnlUsd() != null && trade.realizedPnlUsd().signum() > 0).count();
            long losses = trades.stream().filter(trade -> trade.realizedPnlUsd() != null && trade.realizedPnlUsd().signum() < 0).count();
            List<BigDecimal> winsValues = trades.stream().map(PaperValidationTrade::realizedPnlUsd).filter(value -> value != null && value.signum() > 0).toList();
            List<BigDecimal> lossesValues = trades.stream().map(PaperValidationTrade::realizedPnlUsd).filter(value -> value != null && value.signum() < 0).toList();
            BigDecimal grossWins = winsValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal grossLosses = lossesValues.stream().map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal closed = BigDecimal.valueOf(wins + losses);
            return new PaperValidationResult(request.initialCashUsd(), cash, totalReturn,
                    percent(totalReturn, request.initialCashUsd()), realized, unrealized, maxDrawdown,
                    percent(maxDrawdown, peak.signum() == 0 ? request.initialCashUsd() : peak), trades.size(), (int) wins, (int) losses,
                    closed.signum() == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(wins).divide(closed, 8, RoundingMode.HALF_UP),
                    average(winsValues), average(lossesValues), grossLosses.signum() == 0 ? (grossWins.signum() == 0 ? BigDecimal.ZERO : null) : grossWins.divide(grossLosses, 8, RoundingMode.HALF_UP),
                    exposurePoints == 0 ? BigDecimal.ZERO : exposureTotal.divide(BigDecimal.valueOf(exposurePoints), 8, RoundingMode.HALF_UP),
                    closedPositions == 0 ? BigDecimal.ZERO : holdingMinutesTotal.divide(BigDecimal.valueOf(closedPositions), 8, RoundingMode.HALF_UP),
                    equityCurve, trades, algorithmVersions, returnByType, returnByToken);
        }

        private Position simulateEntry(ReplaySignal signal, ReplayPricePoint price) {
            BigDecimal fee = request.tradeNotionalUsd().multiply(request.feePercent()).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
            BigDecimal executionPrice = price.priceUsd().multiply(BigDecimal.ONE.add(rate(request.slippageBps())));
            BigDecimal quantity = request.tradeNotionalUsd().subtract(fee).divide(executionPrice, SCALE, RoundingMode.DOWN);
            BigDecimal slippage = executionPrice.subtract(price.priceUsd()).multiply(quantity).max(BigDecimal.ZERO);
            return new Position(signal, price, executionPrice, quantity, fee, slippage, request.tradeNotionalUsd());
        }

        private void closePositions(ReplayPricePoint price) {
            for (int index = open.size() - 1; index >= 0; index--) {
                Position position = open.get(index);
                if (!position.signal.tokenAddress().equals(price.tokenAddress()) || !position.signal.detectedAt().isBefore(price.observedAt())) continue;
                String reason = exitReason(position, price);
                if (reason == null) continue;
                PaperValidationTrade trade = position.close(price, reason, request);
                cash = cash.add(trade.exitNotionalUsd());
                trades.add(trade);
                returnByType.merge(position.signal.signalType(), trade.realizedPnlUsd(), BigDecimal::add);
                returnByToken.merge(position.signal.tokenAddress(), trade.realizedPnlUsd(), BigDecimal::add);
                open.remove(index);
            }
        }

        private String exitReason(Position position, ReplayPricePoint price) {
            BigDecimal change = price.priceUsd().subtract(position.rawEntryPrice).divide(position.rawEntryPrice, SCALE, RoundingMode.HALF_UP).multiply(HUNDRED);
            if (request.takeProfitPercent().signum() > 0 && change.compareTo(request.takeProfitPercent()) >= 0) return "TAKE_PROFIT";
            if (request.stopLossPercent().signum() > 0 && change.compareTo(request.stopLossPercent().negate()) <= 0) return "STOP_LOSS";
            if (position.signal.expiresAt() != null && !price.observedAt().isBefore(position.signal.expiresAt())) return "SIGNAL_EXPIRY";
            if (price.observedAt().equals(request.to())) return "PERIOD_END";
            return null;
        }

        private void recordEquity(ReplayPricePoint price) {
            BigDecimal equity = equityAt(price);
            if (equity.compareTo(peak) > 0) peak = equity;
            BigDecimal drawdown = peak.subtract(equity).max(BigDecimal.ZERO);
            maxDrawdown = maxDrawdown.max(drawdown);
            BigDecimal exposure = equity.signum() == 0 ? BigDecimal.ZERO : equity.subtract(cash).divide(equity, 8, RoundingMode.HALF_UP).multiply(HUNDRED);
            exposureTotal = exposureTotal.add(exposure); exposurePoints++;
            equityCurve.add(new EquityPoint(price.observedAt(), cash, equity, drawdown));
        }

        private BigDecimal equityAt(ReplayPricePoint price) {
            return cash.add(open.stream().map(position -> {
                ReplayPricePoint mark = prices.stream()
                        .filter(candidate -> candidate.tokenAddress().equals(position.signal.tokenAddress()))
                        .filter(candidate -> !candidate.observedAt().isAfter(price.observedAt()))
                        .max(Comparator.comparing(ReplayPricePoint::observedAt)).orElse(null);
                return mark == null ? BigDecimal.ZERO : unrealizedValue(position, mark);
            }).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        private BigDecimal unrealizedAt(Position position, ReplayPricePoint price) {
            ReplayPricePoint mark = prices.stream()
                    .filter(candidate -> candidate.tokenAddress().equals(position.signal.tokenAddress()))
                    .filter(candidate -> price != null && !candidate.observedAt().isAfter(price.observedAt()))
                    .max(Comparator.comparing(ReplayPricePoint::observedAt)).orElse(null);
            return mark == null ? BigDecimal.ZERO : unrealizedValue(position, mark).subtract(position.entryNotional);
        }
        private BigDecimal unrealizedValue(Position position, ReplayPricePoint price) {
            if (!position.signal.tokenAddress().equals(price.tokenAddress())) return BigDecimal.ZERO;
            BigDecimal sellPrice = price.priceUsd().multiply(BigDecimal.ONE.subtract(rate(request.slippageBps())));
            BigDecimal gross = position.quantity.multiply(sellPrice);
            return gross.subtract(gross.multiply(request.feePercent()).divide(HUNDRED, SCALE, RoundingMode.HALF_UP));
        }
        private boolean isExpired(ReplaySignal signal, Instant at) { return signal.expiresAt() != null && at.isAfter(signal.expiresAt()); }
        private static BigDecimal rate(int bps) { return BigDecimal.valueOf(bps).divide(BigDecimal.valueOf(10_000), SCALE, RoundingMode.HALF_UP); }
        private static BigDecimal percent(BigDecimal value, BigDecimal base) { return base.signum() == 0 ? BigDecimal.ZERO : value.divide(base, 8, RoundingMode.HALF_UP).multiply(HUNDRED); }
        private static BigDecimal average(List<BigDecimal> values) { return values.isEmpty() ? BigDecimal.ZERO : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP); }
    }

    private static final class Position {
        private final ReplaySignal signal;
        private final ReplayPricePoint entry;
        private final BigDecimal quantity;
        private final BigDecimal entryFee;
        private final BigDecimal entrySlippage;
        private final BigDecimal entryNotional;
        private final BigDecimal rawEntryPrice;
        private final BigDecimal executionEntryPrice;
        private Position(ReplaySignal signal, ReplayPricePoint entry, BigDecimal executionEntryPrice, BigDecimal quantity, BigDecimal entryFee, BigDecimal entrySlippage, BigDecimal entryNotional) {
            this.signal = signal; this.entry = entry; this.quantity = quantity; this.entryFee = entryFee; this.entrySlippage = entrySlippage; this.entryNotional = entryNotional; this.rawEntryPrice = entry.priceUsd(); this.executionEntryPrice = executionEntryPrice;
        }
        private PaperValidationTrade close(ReplayPricePoint exit, String reason, PaperValidationRequest request) {
            BigDecimal sellPrice = exit.priceUsd().multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(request.slippageBps()).divide(BigDecimal.valueOf(10_000), 12, RoundingMode.HALF_UP)));
            BigDecimal gross = quantity.multiply(sellPrice);
            BigDecimal exitFee = gross.multiply(request.feePercent()).divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP);
            BigDecimal net = gross.subtract(exitFee);
            BigDecimal sellSlippage = exit.priceUsd().subtract(sellPrice).multiply(quantity).max(BigDecimal.ZERO);
            return new PaperValidationTrade(signal.signalId(), signal.tokenAddress(), signal.detectedAt(), executionEntryPrice, exit.priceUsd(), quantity,
                    entryNotional, net, entryFee.add(exitFee), entrySlippage.add(sellSlippage), net.subtract(entryNotional), reason,
                    signal.evidence());
        }
        private PaperValidationTrade openTrade() { return new PaperValidationTrade(signal.signalId(), signal.tokenAddress(), signal.detectedAt(), executionEntryPrice, null, quantity, entryNotional, null, entryFee, entrySlippage, null, "OPEN", signal.evidence()); }
    }
}
