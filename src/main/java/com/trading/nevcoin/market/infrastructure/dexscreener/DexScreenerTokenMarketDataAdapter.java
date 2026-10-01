package com.trading.nevcoin.market.infrastructure.dexscreener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.nevcoin.notification.application.ports.TokenMarketDataPort;
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
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Component
public class DexScreenerTokenMarketDataAdapter implements TokenMarketDataPort {

    private static final Logger log = LoggerFactory.getLogger(DexScreenerTokenMarketDataAdapter.class);

    private final DexScreenerProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DexScreenerTokenMarketDataAdapter(DexScreenerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public Optional<TokenMarketData> find(String mintAddress) {
        if (!properties.isEnabled() || mintAddress == null || mintAddress.isBlank()) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(uri(mintAddress))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("DEX Screener market lookup returned HTTP {}", response.statusCode());
                return Optional.empty();
            }
            return parse(objectMapper.readTree(response.body()), mintAddress);
        } catch (IOException exception) {
            log.warn("DEX Screener market lookup failed", exception);
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (RuntimeException exception) {
            log.warn("DEX Screener market response could not be processed", exception);
            return Optional.empty();
        }
    }

    Optional<TokenMarketData> parse(JsonNode response, String mintAddress) {
        if (!response.isArray()) {
            return Optional.empty();
        }

        return StreamSupport.stream(response.spliterator(), false)
                .filter(pair -> mintAddress.equals(pair.path("baseToken").path("address").asText()))
                .max(Comparator.comparing(this::liquidityUsd))
                .map(pair -> toMarketData(pair, mintAddress));
    }

    private TokenMarketData toMarketData(JsonNode pair, String mintAddress) {
        String dexId = text(pair.path("dexId"));
        String source = dexId == null ? "DEX Screener" : "DEX Screener / " + dexId;
        return new TokenMarketData(
                mintAddress,
                text(pair.path("baseToken").path("symbol")),
                text(pair.path("baseToken").path("name")),
                decimal(pair.path("priceUsd")),
                decimal(pair.path("marketCap")),
                liquidityUsd(pair),
                decimal(pair.path("volume").path("h24")),
                decimal(pair.path("priceChange").path("h24")),
                pair.path("txns").path("m5").path("buys").asInt(0),
                pair.path("txns").path("m5").path("sells").asInt(0),
                Instant.now(),
                source);
    }

    private BigDecimal liquidityUsd(JsonNode pair) {
        BigDecimal value = decimal(pair.path("liquidity").path("usd"));
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal decimal(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String text(JsonNode value) {
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private URI uri(String mintAddress) {
        String baseUrl = properties.getBaseUrl().replaceAll("/$", "");
        String chainId = URLEncoder.encode(properties.getChainId(), StandardCharsets.UTF_8);
        String token = URLEncoder.encode(mintAddress.trim(), StandardCharsets.UTF_8);
        return URI.create(baseUrl + "/token-pairs/v1/" + chainId + "/" + token);
    }
}
