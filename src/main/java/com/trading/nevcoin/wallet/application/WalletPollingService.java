package com.trading.nevcoin.wallet.application;

import com.trading.nevcoin.notification.application.ports.NotificationPort;
import com.trading.nevcoin.wallet.application.ports.WalletActivityProvider;
import com.trading.nevcoin.wallet.application.ports.WalletRegistryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class WalletPollingService {
    private static final Logger log = LoggerFactory.getLogger(WalletPollingService.class);
    private final WalletProperties properties;
    private final WalletRegistryStore registry;
    private final WalletActivityProvider provider;
    private final WalletSwapDetector detector;
    private final NotificationPort notificationPort;
    private final Set<String> seenSignatures = new HashSet<>();

    public WalletPollingService(WalletProperties properties, WalletRegistryStore registry, WalletActivityProvider provider,
                                WalletSwapDetector detector, NotificationPort notificationPort) {
        this.properties = properties; this.registry = registry; this.provider = provider;
        this.detector = detector; this.notificationPort = notificationPort;
    }

    @Scheduled(fixedDelayString = "${wallet.poll-interval-ms:60000}")
    public void pollTrackedWallets() {
        if (!properties.isEnabled()) return;
        for (var wallet : registry.findAll()) {
            try {
                for (var transaction : provider.recentTransactions(wallet.address(), 20)) {
                    if (!markSeen(transaction.signature())) continue;
                    detector.detect(transaction).forEach(trade -> {
                        log.info("Wallet swap observed wallet={} token={} side={} signature={}",
                                trade.walletAddress(), trade.tokenAddress(), trade.side(), trade.transactionSignature());
                        if (properties.isAlertsEnabled()) {
                            String message = "WALLET ACTIVITY\n\nWallet: %s\nAction: %s\nToken: %s\nAmount: %s\nTx: %s\n\nDescriptive intelligence only."
                                    .formatted(trade.walletAddress(), trade.side(), trade.tokenAddress(), trade.amountToken(), trade.transactionSignature());
                            properties.parsedAlertChatIds().forEach(chatId -> notificationPort.send(new NotificationPort.Notification(chatId, message)));
                        }
                    });
                }
            } catch (RuntimeException exception) {
                log.warn("Wallet polling failed address={}", wallet.address(), exception);
            }
        }
    }

    private synchronized boolean markSeen(String signature) {
        if (!seenSignatures.add(signature)) return false;
        if (seenSignatures.size() > 10_000) seenSignatures.clear();
        return true;
    }
}
