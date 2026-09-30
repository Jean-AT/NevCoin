package com.trading.nevcoin.wallet.domain;

import java.math.BigDecimal;

public record WalletPosition(String walletAddress, String tokenAddress, BigDecimal quantity, BigDecimal costBasisUsd) { }
