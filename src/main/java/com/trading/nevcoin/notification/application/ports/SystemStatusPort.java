package com.trading.nevcoin.notification.application.ports;

public interface SystemStatusPort {

    SystemStatus current();

    record SystemStatus(String health, String mode) {
    }
}
