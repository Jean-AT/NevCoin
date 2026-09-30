package com.trading.nevcoin.wallet.application;

import com.trading.nevcoin.wallet.domain.WalletPosition;
import com.trading.nevcoin.wallet.domain.WalletTrade;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WalletPositionCalculator {
    public List<WalletPosition> calculate(String walletAddress, List<WalletTrade> trades) {
        Map<String, BigDecimal> quantities = new HashMap<>();
        for (WalletTrade trade : trades) {
            BigDecimal amount = trade.amountToken() == null ? BigDecimal.ZERO : trade.amountToken();
            quantities.merge(trade.tokenAddress(), trade.side() == WalletTrade.Side.BUY ? amount : amount.negate(), BigDecimal::add);
        }
        return quantities.entrySet().stream().filter(entry -> entry.getValue().signum() != 0)
                .map(entry -> new WalletPosition(walletAddress, entry.getKey(), entry.getValue(), BigDecimal.ZERO)).toList();
    }
}
