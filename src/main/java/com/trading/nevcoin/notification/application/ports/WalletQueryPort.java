package com.trading.nevcoin.notification.application.ports;

import java.util.List;

public interface WalletQueryPort {
    List<WalletSummary> list();
    WalletSummary find(String address);
    record WalletSummary(String address, String alias, String status) { }
}
