package com.trading.nevcoin.wallet.application;

import com.trading.nevcoin.notification.application.ports.WalletQueryPort;
import com.trading.nevcoin.wallet.application.ports.WalletActivityProvider;
import com.trading.nevcoin.wallet.application.ports.WalletRegistryStore;
import com.trading.nevcoin.wallet.domain.TrackedWallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WalletIntelligenceService implements WalletQueryPort, com.trading.nevcoin.notification.application.ports.WalletWatchlistCommandPort {
    private final WalletRegistryStore store;

    public WalletIntelligenceService(WalletRegistryStore store) { this.store = store; }

    @Override @Transactional public void watch(String address) { store.save(new TrackedWallet(requireAddress(address), null, TrackedWallet.Status.ACTIVE, Instant.now())); }
    @Override @Transactional public void unwatch(String address) { store.delete(requireAddress(address)); }
    @Override @Transactional(readOnly = true) public java.util.List<WalletQueryPort.WalletSummary> list() {
        return store.findAll().stream().map(w -> new WalletQueryPort.WalletSummary(w.address(), w.alias(), w.status().name())).toList();
    }
    @Override @Transactional(readOnly = true) public WalletQueryPort.WalletSummary find(String address) {
        return store.find(requireAddress(address)).map(w -> new WalletQueryPort.WalletSummary(w.address(), w.alias(), w.status().name())).orElse(null);
    }
    private String requireAddress(String address) {
        if (address == null || address.isBlank() || address.chars().anyMatch(Character::isWhitespace)) throw new IllegalArgumentException("A valid wallet address is required");
        return address.trim();
    }
}
