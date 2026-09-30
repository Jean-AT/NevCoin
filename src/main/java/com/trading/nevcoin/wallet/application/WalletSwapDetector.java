package com.trading.nevcoin.wallet.application;

import com.trading.nevcoin.wallet.domain.WalletTrade;
import com.trading.nevcoin.wallet.domain.WalletTransaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class WalletSwapDetector {
    public List<WalletTrade> detect(WalletTransaction transaction) {
        var increases = transaction.tokenTransfers().stream().filter(t -> "INCREASE".equals(t.direction())).toList();
        var decreases = transaction.tokenTransfers().stream().filter(t -> "DECREASE".equals(t.direction())).toList();
        if (increases.isEmpty() || decreases.isEmpty()) return List.of();
        return increases.stream().map(token -> new WalletTrade(transaction.walletAddress(), token.mintAddress(),
                WalletTrade.Side.BUY, decimal(token.amount()), null, null, transaction.signature(), transaction.sourceTimestamp())).toList();
    }

    private BigDecimal decimal(String value) { return value == null ? BigDecimal.ZERO : new BigDecimal(value); }
}
