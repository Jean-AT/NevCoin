package com.trading.nevcoin.wallet.domain;

import java.time.Instant;
import java.util.List;

public record WalletTransaction(
        String walletAddress,
        String signature,
        Instant observedAt,
        Instant sourceTimestamp,
        boolean successful,
        long feeLamports,
        List<TokenTransfer> tokenTransfers,
        String sourceQuality) {

    public WalletTransaction {
        tokenTransfers = List.copyOf(tokenTransfers);
    }

    public record TokenTransfer(
            String mintAddress,
            String owner,
            String amount,
            int decimals,
            String direction) { }
}
