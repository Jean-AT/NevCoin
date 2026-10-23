package com.trading.nevcoin.signal.application.ports;

import java.math.BigDecimal;
import java.time.Instant;

/** Provider-independent wallet event consumed by Signal Intelligence. */
public record WalletSignalEvent(String walletAddress, String tokenAddress, String transactionSignature,
                                BigDecimal amountUsd, Instant observedAt) { }
