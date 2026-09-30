package com.trading.nevcoin.wallet.application;

import com.trading.nevcoin.wallet.domain.WalletTrade;
import com.trading.nevcoin.wallet.domain.WalletTransaction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletIntelligenceTest {
    @Test
    void detectsSwapOnlyWhenBothTokenDirectionsExist() {
        var tx = new WalletTransaction("wallet", "signature", Instant.now(), Instant.now(), true, 1,
                List.of(new WalletTransaction.TokenTransfer("token", "wallet", "2", 6, "INCREASE"),
                        new WalletTransaction.TokenTransfer("usdc", "wallet", "10", 6, "DECREASE")), "test");
        var trades = new WalletSwapDetector().detect(tx);
        assertEquals(1, trades.size());
        assertEquals(WalletTrade.Side.BUY, trades.getFirst().side());
        assertEquals("2", trades.getFirst().amountToken().toPlainString());
    }

    @Test
    void profileConfidenceReflectsSampleSize() {
        var calculator = new WalletProfileCalculator();
        var trade = new WalletTrade("wallet", "token", WalletTrade.Side.BUY,
                java.math.BigDecimal.ONE, null, null, "sig", Instant.now());
        var profile = calculator.calculate("wallet", List.of(trade), Instant.now());
        assertEquals(0, profile.confidence().compareTo(java.math.BigDecimal.valueOf(0.20).setScale(2)));
        assertEquals(1, profile.sampleSize());
    }
}
