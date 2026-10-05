package com.trading.nevcoin.market.infrastructure.helius;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trading.nevcoin.market.application.MarketProperties;
import com.trading.nevcoin.market.application.ports.MarketTradeStreamProvider;
import com.trading.nevcoin.market.domain.MarketTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Component
public class HeliusMarketTradeStreamProvider implements MarketTradeStreamProvider {

    private static final Logger log = LoggerFactory.getLogger(HeliusMarketTradeStreamProvider.class);

    private final MarketProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService reconnectExecutor;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong requestIds = new AtomicLong(1);
    private final Map<Long, String> pendingSubscriptions = new HashMap<>();
    private final Map<Long, Long> pendingUnsubscriptions = new HashMap<>();
    private final Map<Long, String> subscriptionTokens = new HashMap<>();
    private final Set<String> seenSignatures = new HashSet<>();
    private volatile Set<String> tokenAddresses = Set.of();
    private volatile Consumer<MarketTrade> consumer = ignored -> { };
    private volatile java.net.http.WebSocket webSocket;

    public HeliusMarketTradeStreamProvider(MarketProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "helius-market-stream-reconnect");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public synchronized void start(Set<String> tokenAddresses, Consumer<MarketTrade> consumer) {
        if (running.get()) {
            return;
        }
        if (properties.getHeliusApiKey() == null || properties.getHeliusApiKey().isBlank()) {
            throw new IllegalStateException("Market stream is enabled but HELIUS_API_KEY is empty");
        }
        this.tokenAddresses = Set.copyOf(tokenAddresses);
        this.consumer = consumer;
        running.set(true);
        connect();
    }

    @Override
    public synchronized void updateSubscriptions(Set<String> tokenAddresses) {
        Set<String> desiredTokens = Set.copyOf(tokenAddresses);
        if (desiredTokens.equals(this.tokenAddresses)) {
            return;
        }
        this.tokenAddresses = desiredTokens;
        java.net.http.WebSocket current = webSocket;
        if (running.get() && current != null) {
            reconcileSubscriptions(current);
        }
    }

    @Override
    public synchronized void stop() {
        running.set(false);
        java.net.http.WebSocket current = webSocket;
        webSocket = null;
        if (current != null) {
            current.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "shutdown");
        }
        synchronized (pendingSubscriptions) {
            pendingSubscriptions.clear();
            pendingUnsubscriptions.clear();
            subscriptionTokens.clear();
        }
    }

    private void connect() {
        if (!running.get()) {
            return;
        }
        try {
            httpClient.newWebSocketBuilder()
                    .buildAsync(webSocketUri(), new Listener())
                    .whenComplete((socket, error) -> {
                        if (error != null) {
                            log.warn("Helius market stream connection failed; retrying", error);
                            scheduleReconnect();
                        } else {
                            webSocket = socket;
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn("Helius market stream connection could not be created; retrying", exception);
            scheduleReconnect();
        }
    }

    private URI webSocketUri() {
        String apiKey = URLEncoder.encode(properties.getHeliusApiKey(), StandardCharsets.UTF_8);
        String separator = properties.getStreamUrl().contains("?") ? "&" : "?";
        return URI.create(properties.getStreamUrl() + separator + "api-key=" + apiKey);
    }

    private void scheduleReconnect() {
        if (running.get() && !reconnectExecutor.isShutdown()) {
            reconnectExecutor.schedule(this::connect,
                    properties.getStreamReconnectDelaySeconds(), TimeUnit.SECONDS);
        }
    }

    private void subscribe(java.net.http.WebSocket socket) {
        synchronized (pendingSubscriptions) {
            pendingSubscriptions.clear();
            pendingUnsubscriptions.clear();
            subscriptionTokens.clear();
            tokenAddresses.forEach(tokenAddress -> sendSubscribe(socket, tokenAddress));
        }
    }

    private void reconcileSubscriptions(java.net.http.WebSocket socket) {
        synchronized (pendingSubscriptions) {
            Set<String> subscribedOrPending = new HashSet<>(subscriptionTokens.values());
            subscribedOrPending.addAll(pendingSubscriptions.values());
            tokenAddresses.stream()
                    .filter(tokenAddress -> !subscribedOrPending.contains(tokenAddress))
                    .forEach(tokenAddress -> sendSubscribe(socket, tokenAddress));

            subscriptionTokens.entrySet().stream()
                    .filter(entry -> !tokenAddresses.contains(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .toList()
                    .forEach(subscriptionId -> sendUnsubscribe(socket, subscriptionId));
        }
    }

    private void sendSubscribe(java.net.http.WebSocket socket, String tokenAddress) {
        long requestId = requestIds.getAndIncrement();
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", requestId);
        request.put("method", properties.getStreamChannel());
        ArrayNode params = request.putArray("params");
        ObjectNode filter = params.addObject();
        filter.putArray("mentions").add(tokenAddress);
        params.addObject().put("commitment", "confirmed");
        pendingSubscriptions.put(requestId, tokenAddress);
        socket.sendText(request.toString(), true);
    }

    private void sendUnsubscribe(java.net.http.WebSocket socket, long subscriptionId) {
        long requestId = requestIds.getAndIncrement();
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", requestId);
        request.put("method", "logsUnsubscribe");
        request.putArray("params").add(subscriptionId);
        subscriptionTokens.remove(subscriptionId);
        pendingUnsubscriptions.put(requestId, subscriptionId);
        socket.sendText(request.toString(), true);
    }

    private void handleSubscriptionResponse(JsonNode root) {
        long requestId = root.path("id").asLong();
        synchronized (pendingSubscriptions) {
            String token = pendingSubscriptions.remove(requestId);
            if (token != null) {
                if (!root.path("result").isNumber()) {
                    log.warn("Helius rejected market stream subscription for tokenAddress={}", token);
                    return;
                }
                long subscriptionId = root.path("result").asLong();
                if (tokenAddresses.contains(token)) {
                    subscriptionTokens.put(subscriptionId, token);
                } else if (webSocket != null) {
                    sendUnsubscribe(webSocket, subscriptionId);
                }
                return;
            }
            pendingUnsubscriptions.remove(requestId);
        }
    }

    private void handleMessage(String text) {
        try {
            JsonNode root = objectMapper.readTree(text);
            if (root.has("id") && (root.has("result") || root.has("error"))) {
                handleSubscriptionResponse(root);
                return;
            }
            if (!"logsNotification".equals(root.path("method").asText())) {
                return;
            }
            long subscriptionId = root.path("params").path("subscription").asLong(-1);
            String tokenAddress;
            synchronized (pendingSubscriptions) {
                tokenAddress = subscriptionTokens.get(subscriptionId);
            }
            String signature = root.path("params").path("result").path("value").path("signature").asText(null);
            if (tokenAddress != null && signature != null && markUnseen(signature)) {
                fetchTransaction(tokenAddress, signature);
            }
        } catch (IOException exception) {
            log.debug("Ignoring malformed Helius stream message", exception);
        }
    }

    private boolean markUnseen(String signature) {
        synchronized (seenSignatures) {
            if (!seenSignatures.add(signature)) {
                return false;
            }
            if (seenSignatures.size() > 10_000) {
                seenSignatures.clear();
                seenSignatures.add(signature);
            }
            return true;
        }
    }

    private void fetchTransaction(String tokenAddress, String signature) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("id", requestIds.getAndIncrement());
        requestBody.put("method", "getTransaction");
        ArrayNode params = requestBody.putArray("params");
        params.add(signature);
        ObjectNode options = params.addObject();
        options.put("encoding", "jsonParsed");
        options.put("commitment", "confirmed");
        options.put("maxSupportedTransactionVersion", 0);

        HttpRequest request = HttpRequest.newBuilder(httpUri())
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(body -> emitTrade(tokenAddress, signature, body))
                .exceptionally(error -> {
                    log.debug("Could not fetch Helius transaction signature={}", signature, error);
                    return null;
                });
    }

    private URI httpUri() {
        String apiKey = URLEncoder.encode(properties.getHeliusApiKey(), StandardCharsets.UTF_8);
        String base = properties.getStreamUrl().replaceFirst("^wss", "https");
        String separator = base.contains("?") ? "&" : "?";
        return URI.create(base + separator + "api-key=" + apiKey);
    }

    private void emitTrade(String tokenAddress, String signature, String body) {
        try {
            JsonNode transaction = objectMapper.readTree(body).path("result");
            if (transaction.isMissingNode() || transaction.isNull()) {
                return;
            }
            JsonNode meta = transaction.path("meta");
            JsonNode postBalances = meta.path("postTokenBalances");
            JsonNode preBalances = meta.path("preTokenBalances");
            TokenChange change = largestTokenChange(tokenAddress, preBalances, postBalances);
            if (change == null || change.delta().signum() == 0) {
                return;
            }
            Instant sourceTimestamp = transaction.hasNonNull("blockTime")
                    ? Instant.ofEpochSecond(transaction.path("blockTime").asLong())
                    : Instant.now();
            MarketTrade.Side side = change.delta().signum() > 0
                    ? MarketTrade.Side.BUY : MarketTrade.Side.SELL;
            consumer.accept(new MarketTrade(tokenAddress, Instant.now(), sourceTimestamp,
                    signature, change.owner(), side, null, null, "helius-logs"));
        } catch (Exception exception) {
            log.debug("Ignoring unparseable Helius transaction signature={}", signature, exception);
        }
    }

    private TokenChange largestTokenChange(String tokenAddress, JsonNode preBalances, JsonNode postBalances) {
        Map<String, BigDecimal> before = balancesByOwner(tokenAddress, preBalances);
        Map<String, BigDecimal> after = balancesByOwner(tokenAddress, postBalances);
        Set<String> owners = new HashSet<>(before.keySet());
        owners.addAll(after.keySet());
        TokenChange largest = null;
        for (String owner : owners) {
            BigDecimal delta = after.getOrDefault(owner, BigDecimal.ZERO)
                    .subtract(before.getOrDefault(owner, BigDecimal.ZERO));
            if (delta.signum() != 0 && (largest == null || delta.abs().compareTo(largest.delta().abs()) > 0)) {
                largest = new TokenChange(owner, delta);
            }
        }
        return largest;
    }

    private Map<String, BigDecimal> balancesByOwner(String tokenAddress, JsonNode balances) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (!balances.isArray()) {
            return result;
        }
        for (JsonNode balance : balances) {
            if (!tokenAddress.equals(balance.path("mint").asText())) {
                continue;
            }
            String owner = balance.path("owner").asText(null);
            if (owner == null) {
                continue;
            }
            JsonNode uiAmountString = balance.path("uiTokenAmount").path("uiAmountString");
            BigDecimal amount = uiAmountString.isTextual()
                    ? new BigDecimal(uiAmountString.asText())
                    : BigDecimal.valueOf(balance.path("uiTokenAmount").path("uiAmount").asDouble(0));
            result.merge(owner, amount, BigDecimal::add);
        }
        return result;
    }

    private record TokenChange(String owner, BigDecimal delta) { }

    private final class Listener implements java.net.http.WebSocket.Listener {
        private final StringBuilder message = new StringBuilder();

        @Override
        public void onOpen(java.net.http.WebSocket socket) {
            webSocket = socket;
            log.info("Helius market stream connected");
            subscribe(socket);
            java.net.http.WebSocket.Listener.super.onOpen(socket);
        }

        @Override
        public CompletionStage<?> onText(java.net.http.WebSocket socket, CharSequence data, boolean last) {
            message.append(data);
            if (last) {
                handleMessage(message.toString());
                message.setLength(0);
            }
            return java.net.http.WebSocket.Listener.super.onText(socket, data, last);
        }

        @Override
        public void onError(java.net.http.WebSocket socket, Throwable error) {
            log.warn("Helius market stream error; reconnecting", error);
            scheduleReconnect();
        }

        @Override
        public CompletionStage<?> onClose(java.net.http.WebSocket socket, int statusCode, String reason) {
            log.warn("Helius market stream closed statusCode={} reason={}", statusCode, reason);
            scheduleReconnect();
            return java.net.http.WebSocket.Listener.super.onClose(socket, statusCode, reason);
        }
    }
}
