package com.trading.nevcoin.market.infrastructure.birdeye;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.nevcoin.market.application.MarketProperties;
import com.trading.nevcoin.market.application.ports.MarketDataProvider;
import com.trading.nevcoin.market.domain.MarketTick;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Component
public class BirdeyeMarketDataProvider implements MarketDataProvider {

    private final MarketProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public BirdeyeMarketDataProvider(MarketProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public MarketTick fetch(String tokenAddress) {
        if (properties.getBirdeyeApiKey() == null || properties.getBirdeyeApiKey().isBlank()) {
            throw new BirdeyeMarketDataException("BIRDEYE_API_KEY is required when market ingestion is enabled");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl(tokenAddress)))
                .timeout(Duration.ofSeconds(20))
                .header("X-API-KEY", properties.getBirdeyeApiKey())
                .header("x-chain", properties.getChain())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BirdeyeMarketDataException("Birdeye API returned HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.path("success").asBoolean(false) && root.has("success")) {
                throw new BirdeyeMarketDataException("Birdeye API rejected the market request");
            }
            return toMarketTick(tokenAddress, root.path("data"));
        } catch (IOException exception) {
            throw new BirdeyeMarketDataException("Birdeye response could not be read", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BirdeyeMarketDataException("Birdeye request interrupted", exception);
        }
    }

    private MarketTick toMarketTick(String tokenAddress, JsonNode data) {
        Instant observedAt = Instant.now();
        Instant sourceTimestamp = data.hasNonNull("lastTradeUnixTime")
                ? Instant.ofEpochSecond(data.path("lastTradeUnixTime").longValue())
                : observedAt;
        return new MarketTick(
                tokenAddress,
                observedAt,
                sourceTimestamp,
                decimal(data, "price"),
                decimal(data, "liquidity"),
                decimal(data, "volume24hUSD"),
                decimal(data, "priceChange24hPercent"),
                "birdeye-token-overview");
    }

    private BigDecimal decimal(JsonNode data, String field) {
        JsonNode value = data.get(field);
        return value == null || value.isNull() ? null : value.decimalValue();
    }

    private String apiUrl(String tokenAddress) {
        String baseUrl = properties.getBirdeyeBaseUrl().replaceAll("/$", "");
        return baseUrl + "/defi/token_overview?address=" + tokenAddress;
    }

    public static class BirdeyeMarketDataException extends RuntimeException {

        public BirdeyeMarketDataException(String message) {
            super(message);
        }

        public BirdeyeMarketDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
