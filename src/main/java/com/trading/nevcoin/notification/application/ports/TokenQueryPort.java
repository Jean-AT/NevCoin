package com.trading.nevcoin.notification.application.ports;

import java.time.Instant;
import java.util.List;

public interface TokenQueryPort {
    List<TokenSummary> list();
    TokenSummary find(String query);

    record TokenSummary(String mintAddress, String symbol, String name, String status, Instant firstSeenAt) { }
}
