package com.trading.nevcoin.trade.infrastructure.persistence;

import com.trading.nevcoin.trade.application.PaperTradingProperties;
import com.trading.nevcoin.trade.application.ports.PaperPortfolioStore;
import com.trading.nevcoin.trade.domain.PaperPosition;
import com.trading.nevcoin.trade.domain.PaperTrade;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaPaperPortfolioStore implements PaperPortfolioStore {

    private final PaperSettingsJpaRepository settingsRepository;
    private final PaperPositionJpaRepository positionRepository;
    private final PaperTradeJpaRepository tradeRepository;

    public JpaPaperPortfolioStore(
            PaperSettingsJpaRepository settingsRepository,
            PaperPositionJpaRepository positionRepository,
            PaperTradeJpaRepository tradeRepository) {
        this.settingsRepository = settingsRepository;
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
    }

    @Override
    @Transactional
    public boolean enabled(boolean defaultEnabled, BigDecimal initialCashUsd) {
        return settings(defaultEnabled, initialCashUsd).isEnabled();
    }

    @Override
    @Transactional
    public void setEnabled(boolean enabled, BigDecimal initialCashUsd) {
        PaperSettingsEntity settings = settings(false, initialCashUsd);
        settings.setEnabled(enabled);
        settingsRepository.save(settings);
    }

    @Override
    @Transactional
    public BigDecimal cashBalance(BigDecimal initialCashUsd) {
        return settings(false, initialCashUsd).getCashUsd();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperPosition> positions() {
        return positionRepository.findAll().stream().map(PaperPositionEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaperPosition> position(String tokenAddress) {
        return positionRepository.findById(tokenAddress).map(PaperPositionEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperTrade> recentTrades(int limit) {
        return tradeRepository.findTop10ByOrderByExecutedAtDesc().stream()
                .limit(limit)
                .map(PaperTradeEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal realizedPnlSince(Instant since) {
        return tradeRepository.realizedPnlSince(since);
    }

    @Override
    @Transactional
    public void buy(PaperTrade trade) {
        PaperSettingsEntity settings = settings(true, trade.notionalUsd());
        if (settings.getCashUsd().compareTo(trade.notionalUsd()) < 0) {
            throw new IllegalStateException("Paper balance is insufficient");
        }
        settings.setCashUsd(settings.getCashUsd().subtract(trade.notionalUsd()));
        settingsRepository.save(settings);

        PaperPosition existing = position(trade.tokenAddress()).orElse(null);
        BigDecimal quantity = existing == null ? trade.quantity() : existing.quantity().add(trade.quantity());
        BigDecimal averagePrice = existing == null
                ? trade.priceUsd()
                : existing.quantity().multiply(existing.averageEntryPriceUsd())
                        .add(trade.quantity().multiply(trade.priceUsd()))
                        .divide(quantity, 12, RoundingMode.HALF_UP);
        positionRepository.save(new PaperPositionEntity(new PaperPosition(
                trade.tokenAddress(), quantity, averagePrice, trade.executedAt())));
        tradeRepository.save(new PaperTradeEntity(trade));
    }

    @Override
    @Transactional
    public void reset(BigDecimal initialCashUsd) {
        positionRepository.deleteAllInBatch();
        tradeRepository.deleteAllInBatch();
        settingsRepository.deleteById(PaperSettingsEntity.DEFAULT_ID);
        settingsRepository.save(new PaperSettingsEntity(false, initialCashUsd));
    }

    @Override
    @Transactional
    public void sell(PaperTrade trade) {
        PaperPosition existing = position(trade.tokenAddress())
                .orElseThrow(() -> new IllegalStateException("Paper position does not exist"));
        if (existing.quantity().compareTo(trade.quantity()) < 0) {
            throw new IllegalStateException("Paper position is insufficient");
        }
        PaperSettingsEntity settings = settings(true, BigDecimal.ZERO);
        settings.setCashUsd(settings.getCashUsd().add(trade.notionalUsd()));
        settingsRepository.save(settings);
        positionRepository.deleteById(trade.tokenAddress());
        BigDecimal realizedPnl = trade.priceUsd().subtract(existing.averageEntryPriceUsd())
                .multiply(trade.quantity());
        tradeRepository.save(new PaperTradeEntity(trade.withRealizedPnlUsd(realizedPnl)));
    }

    private PaperSettingsEntity settings(boolean defaultEnabled, BigDecimal initialCashUsd) {
        return settingsRepository.findById(PaperSettingsEntity.DEFAULT_ID)
                .orElseGet(() -> settingsRepository.save(new PaperSettingsEntity(defaultEnabled, initialCashUsd)));
    }
}
