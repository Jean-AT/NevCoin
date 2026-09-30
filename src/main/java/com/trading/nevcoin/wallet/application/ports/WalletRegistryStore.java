package com.trading.nevcoin.wallet.application.ports;

import com.trading.nevcoin.wallet.domain.TrackedWallet;
import java.util.List;
import java.util.Optional;

public interface WalletRegistryStore {
    TrackedWallet save(TrackedWallet wallet);
    void delete(String address);
    List<TrackedWallet> findAll();
    Optional<TrackedWallet> find(String address);
}
