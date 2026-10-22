package com.trading.nevcoin.social.infrastructure.rss;

import com.trading.nevcoin.social.application.ports.SocialEventProvider;
import com.trading.nevcoin.social.application.ports.SocialFetchResult;
import com.trading.nevcoin.social.domain.RawSocialEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RssSocialEventProvider implements SocialEventProvider {
    private static final Logger log = LoggerFactory.getLogger(RssSocialEventProvider.class);
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    @Override
    public SocialFetchResult fetch(String sourceId, String sourceUrl, String cursor, int limit) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl)).header("Accept", "application/rss+xml, application/atom+xml, application/xml").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new SocialFetchResult(List.of(), cursor, Instant.now(), List.of(
                        new com.trading.nevcoin.social.application.ports.SocialProviderFailure("fetch", "HTTP_" + response.statusCode(), "Feed request failed", response.statusCode() >= 500 || response.statusCode() == 429)));
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(response.body().getBytes(StandardCharsets.UTF_8)));
            List<RawSocialEvent> events = new ArrayList<>();
            var nodes = document.getElementsByTagName("item");
            if (nodes.getLength() == 0) nodes = document.getElementsByTagNameNS("*", "entry");
            for (int i = 0; i < Math.min(nodes.getLength(), Math.max(1, limit)); i++) {
                Node node = nodes.item(i);
                String title = child(node, "title");
                String summary = first(child(node, "description"), child(node, "summary"), child(node, "content"));
                String text = (title + (summary == null ? "" : " — " + summary)).trim();
                String id = first(child(node, "guid"), child(node, "id"), child(node, "link"), sourceUrl + "#" + i);
                Instant observed = parseInstant(first(child(node, "pubDate"), child(node, "published"), child(node, "updated")));
                events.add(new RawSocialEvent("rss", sourceId, child(node, "author"), id,
                        first(child(node, "link"), sourceUrl), observed, Instant.now(), text,
                        Map.of("feedUrl", sourceUrl)));
            }
            return new SocialFetchResult(events, cursor, Instant.now(), List.of());
        } catch (Exception exception) {
            log.warn("Social RSS feed failed url={}: {}", sourceUrl, exception.getMessage());
            return new SocialFetchResult(List.of(), cursor, Instant.now(), List.of(
                    new com.trading.nevcoin.social.application.ports.SocialProviderFailure("parse", "INVALID_PAYLOAD", exception.getMessage(), false)));
        }
    }

    private String child(Node parent, String name) {
        if (!(parent instanceof Element element)) return null;
        var nodes = element.getElementsByTagNameNS("*", name);
        if (nodes.getLength() == 0) nodes = element.getElementsByTagName(name);
        if (nodes.getLength() == 0) return null;
        String text = nodes.item(0).getTextContent();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }

    private Instant parseInstant(String value) {
        if (value == null) return Instant.now();
        try { return Instant.parse(value); } catch (DateTimeParseException ignored) { }
        try { return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); } catch (DateTimeParseException ignored) { }
        return Instant.now().minus(1, ChronoUnit.MINUTES);
    }
}
