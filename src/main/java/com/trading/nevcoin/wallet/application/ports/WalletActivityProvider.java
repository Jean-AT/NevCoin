package com.trading.nevcoin.wallet.application.ports;

import com.trading.nevcoin.wallet.domain.WalletTransaction;

import java.util.List;

public interface WalletActivityProvider {
    List<WalletTransaction> recentTransactions(String walletAddress, int limit);
}
