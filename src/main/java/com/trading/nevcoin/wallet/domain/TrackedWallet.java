package com.trading.nevcoin.wallet.domain;

import java.time.Instant;

public record TrackedWallet(String address, String alias, Status status, Instant firstSeenAt) {
    public enum Status { ACTIVE, PAUSED }
}
