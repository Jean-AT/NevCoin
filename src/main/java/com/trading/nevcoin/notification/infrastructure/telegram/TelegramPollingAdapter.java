package com.trading.nevcoin.notification.infrastructure.telegram;

import com.trading.nevcoin.notification.application.TelegramCommandHandler;
import com.trading.nevcoin.notification.application.ports.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true")
public class TelegramPollingAdapter implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TelegramPollingAdapter.class);

    private final TelegramApiClient telegramApiClient;
    private final TelegramCommandHandler commandHandler;
    private final NotificationPort notificationPort;
    private final TelegramProperties properties;
    private final AtomicBoolean running = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "telegram-long-polling");
        thread.setDaemon(true);
        return thread;
    });

    private volatile long nextOffset;

    public TelegramPollingAdapter(
            TelegramApiClient telegramApiClient,
            TelegramCommandHandler commandHandler,
            NotificationPort notificationPort,
            TelegramProperties properties) {
        this.telegramApiClient = telegramApiClient;
        this.commandHandler = commandHandler;
        this.notificationPort = notificationPort;
        this.properties = properties;
    }

    @Override
    public void start() {
        validateConfiguration();
        if (running.compareAndSet(false, true)) {
            executor.submit(this::pollLoop);
            log.info("Telegram long polling started");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Telegram polling thread did not stop within the timeout");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            log.info("Telegram long polling stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                for (TelegramApiClient.TelegramUpdate update : telegramApiClient.getUpdates(
                        nextOffset, properties.getLongPollTimeoutSeconds())) {
                    nextOffset = Math.max(nextOffset, update.updateId() + 1);
                    handleUpdate(update);
                }
            } catch (RuntimeException exception) {
                if (running.get()) {
                    log.warn("Telegram polling failed; retrying after backoff", exception);
                    sleepBackoff();
                }
            }
        }
    }

    private void handleUpdate(TelegramApiClient.TelegramUpdate update) {
        Optional<String> response = commandHandler.handle(update.chatId(), update.text());
        response.ifPresent(text -> notificationPort.send(new NotificationPort.Notification(update.chatId(), text)));
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(properties.getErrorBackoffSeconds() * 1_000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    private void validateConfiguration() {
        if (properties.getBotToken() == null || properties.getBotToken().isBlank()) {
            throw new IllegalStateException("Telegram is enabled but TELEGRAM_BOT_TOKEN is empty");
        }
        if (properties.parsedAllowedChatIds().isEmpty()) {
            throw new IllegalStateException("Telegram is enabled but TELEGRAM_ALLOWED_CHAT_IDS is empty");
        }
    }
}
