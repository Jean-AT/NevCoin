package com.trading.nevcoin.wallet.infrastructure.persistence;
import com.trading.nevcoin.wallet.application.ports.WalletRegistryStore;
import com.trading.nevcoin.wallet.domain.TrackedWallet;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class JpaWalletRegistryStore implements WalletRegistryStore {
    private final TrackedWalletJpaRepository repository;
    public JpaWalletRegistryStore(TrackedWalletJpaRepository repository) { this.repository = repository; }
    public TrackedWallet save(TrackedWallet wallet) { return repository.save(new TrackedWalletEntity(wallet)).toDomain(); }
    public void delete(String address) { repository.deleteById(address); }
    public List<TrackedWallet> findAll() { return repository.findAll().stream().map(TrackedWalletEntity::toDomain).toList(); }
    public Optional<TrackedWallet> find(String address) { return repository.findById(address).map(TrackedWalletEntity::toDomain); }
}
