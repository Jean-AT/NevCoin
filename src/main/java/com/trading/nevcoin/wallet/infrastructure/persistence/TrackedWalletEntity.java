package com.trading.nevcoin.wallet.infrastructure.persistence;

import com.trading.nevcoin.wallet.domain.TrackedWallet;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tracked_wallets")
public class TrackedWalletEntity {
    @Id private String address;
    private String alias;
    @Enumerated(EnumType.STRING) private TrackedWallet.Status status;
    private Instant firstSeenAt;
    protected TrackedWalletEntity() { }
    public TrackedWalletEntity(TrackedWallet wallet) { address = wallet.address(); alias = wallet.alias(); status = wallet.status(); firstSeenAt = wallet.firstSeenAt(); }
    public TrackedWallet toDomain() { return new TrackedWallet(address, alias, status, firstSeenAt); }
}
