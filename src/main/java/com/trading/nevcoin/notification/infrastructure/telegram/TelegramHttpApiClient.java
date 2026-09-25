package com.trading.nevcoin.notification.infrastructure.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramHttpApiClient implements TelegramApiClient {

    private final TelegramProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public TelegramHttpApiClient(TelegramProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) {
        URI uri = apiUri("getUpdates?offset=" + offset + "&timeout=" + timeoutSeconds);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(timeoutSeconds + 10L))
                .GET()
                .build();

        JsonNode response = execute(request);
        List<TelegramUpdate> updates = new ArrayList<>();
        for (JsonNode update : response.path("result")) {
            JsonNode message = update.path("message");
            JsonNode chat = message.path("chat");
            JsonNode text = message.path("text");
            if (update.path("update_id").canConvertToLong()
                    && chat.path("id").canConvertToLong()
                    && text.isTextual()) {
                updates.add(new TelegramUpdate(
                        update.path("update_id").longValue(),
                        chat.path("id").longValue(),
                        text.textValue()));
            }
        }
        return updates;
    }

    @Override
    public void sendMessage(long chatId, String text) {
        String body = objectMapper.createObjectNode()
                .put("chat_id", chatId)
                .put("text", text)
                .toString();
        HttpRequest request = HttpRequest.newBuilder(apiUri("sendMessage"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        execute(request);
    }

    private JsonNode execute(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TelegramApiException("Telegram API returned HTTP " + response.statusCode());
            }

            JsonNode body = objectMapper.readTree(response.body());
            if (!body.path("ok").asBoolean(false)) {
                throw new TelegramApiException("Telegram API rejected the request");
            }
            return body;
        } catch (IOException exception) {
            throw new TelegramApiException("Telegram API request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TelegramApiException("Telegram API request interrupted", exception);
        }
    }

    private URI apiUri(String method) {
        String baseUrl = properties.getApiBaseUrl().replaceAll("/$", "");
        return URI.create(baseUrl + "/bot" + properties.getBotToken() + "/" + method);
    }

    public static class TelegramApiException extends RuntimeException {

        public TelegramApiException(String message) {
            super(message);
        }

        public TelegramApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
