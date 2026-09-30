package com.trading.nevcoin.wallet.infrastructure.helius;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trading.nevcoin.wallet.application.WalletProperties;
import com.trading.nevcoin.wallet.application.ports.WalletActivityProvider;
import com.trading.nevcoin.wallet.domain.WalletTransaction;
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
import java.util.ArrayList;
import java.util.List;

@Component
public class HeliusWalletActivityProvider implements WalletActivityProvider {

    private final WalletProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public HeliusWalletActivityProvider(WalletProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<WalletTransaction> recentTransactions(String walletAddress, int limit) {
        requireAddress(walletAddress);
        int boundedLimit = Math.max(1, Math.min(limit, 100));
        JsonNode signatures = rpc("getSignaturesForAddress", params(walletAddress, boundedLimit)).path("result");
        List<WalletTransaction> transactions = new ArrayList<>();
        for (JsonNode signature : signatures) {
            String value = signature.path("signature").asText(null);
            if (value == null || signature.path("err").isObject()) continue;
            JsonNode transaction = rpc("getTransaction", transactionParams(value)).path("result");
            if (!transaction.isObject()) continue;
            transactions.add(toDomain(walletAddress, value, transaction));
        }
        return List.copyOf(transactions);
    }

    private WalletTransaction toDomain(String walletAddress, String signature, JsonNode transaction) {
        JsonNode meta = transaction.path("meta");
        List<WalletTransaction.TokenTransfer> transfers = new ArrayList<>();
        collectTransfers(transfers, meta.path("preTokenBalances"), meta.path("postTokenBalances"));
        Instant sourceTimestamp = transaction.hasNonNull("blockTime")
                ? Instant.ofEpochSecond(transaction.path("blockTime").asLong()) : Instant.now();
        return new WalletTransaction(walletAddress, signature, Instant.now(), sourceTimestamp,
                meta.path("err").isNull() || meta.path("err").isMissingNode(),
                meta.path("fee").asLong(0), transfers, "helius-rpc");
    }

    private void collectTransfers(List<WalletTransaction.TokenTransfer> transfers, JsonNode pre, JsonNode post) {
        if (!post.isArray()) return;
        for (JsonNode balance : post) {
            String mint = balance.path("mint").asText(null);
            String owner = balance.path("owner").asText(null);
            if (mint == null || owner == null) continue;
            String current = balance.path("uiTokenAmount").path("uiAmountString").asText("0");
            int decimals = balance.path("uiTokenAmount").path("decimals").asInt(0);
            String previous = findPrevious(pre, mint, owner);
            String amount = new BigDecimal(current).subtract(new BigDecimal(previous)).abs().toPlainString();
            String direction = compare(current, previous);
            if (!"UNCHANGED".equals(direction)) {
                transfers.add(new WalletTransaction.TokenTransfer(mint, owner, amount, decimals, direction));
            }
        }
    }

    private String findPrevious(JsonNode pre, String mint, String owner) {
        if (pre.isArray()) for (JsonNode balance : pre) {
            if (mint.equals(balance.path("mint").asText()) && owner.equals(balance.path("owner").asText()))
                return balance.path("uiTokenAmount").path("uiAmountString").asText("0");
        }
        return "0";
    }

    private String compare(String current, String previous) {
        int comparison = new BigDecimal(current).compareTo(new BigDecimal(previous));
        return comparison > 0 ? "INCREASE" : comparison < 0 ? "DECREASE" : "UNCHANGED";
    }

    private JsonNode rpc(String method, ArrayNode params) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("jsonrpc", "2.0"); body.put("id", 1); body.put("method", method); body.set("params", params);
        HttpRequest request = HttpRequest.newBuilder(rpcUri()).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException("Helius RPC returned HTTP " + response.statusCode());
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Helius RPC response could not be read", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Helius RPC request interrupted", exception);
        }
    }

    private ArrayNode params(String address, int limit) {
        ArrayNode params = objectMapper.createArrayNode(); params.add(address);
        ObjectNode options = params.addObject(); options.put("limit", limit); options.put("commitment", "confirmed");
        return params;
    }

    private ArrayNode transactionParams(String signature) {
        ArrayNode params = objectMapper.createArrayNode(); params.add(signature);
        ObjectNode options = params.addObject(); options.put("encoding", "jsonParsed");
        options.put("commitment", "confirmed"); options.put("maxSupportedTransactionVersion", 1);
        return params;
    }

    private URI rpcUri() {
        String key = properties.getHeliusApiKey();
        if (key == null || key.isBlank()) throw new IllegalStateException("HELIUS_API_KEY is empty");
        String separator = properties.getHeliusRpcUrl().contains("?") ? "&" : "?";
        return URI.create(properties.getHeliusRpcUrl() + separator + "api-key=" + URLEncoder.encode(key, StandardCharsets.UTF_8));
    }

    private void requireAddress(String address) {
        if (address == null || address.isBlank() || address.chars().anyMatch(Character::isWhitespace))
            throw new IllegalArgumentException("A wallet address is required");
    }
}
