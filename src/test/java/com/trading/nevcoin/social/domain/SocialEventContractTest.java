package com.trading.nevcoin.social.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SocialEventContractTest {

    @Test
    void rawEventDefaultsMetadataAndReceiveTime() {
        RawSocialEvent event = new RawSocialEvent("rss", "source", "account", "event-1", null,
                Instant.parse("2026-10-21T00:00:00Z"), null, "hello", null);

        assertEquals("rss", event.provider());
        assertEquals("hello", event.rawText());
        assertNotNull(event.receivedAt());
        assertEquals(0, event.metadata().size());
    }

    @Test
    void compatibilityEventKeepsLegacyTokenAndEvidenceViews() {
        SocialEvent event = new SocialEvent("rss:example", "event-1", "mint-1", "author",
                "hello", Instant.parse("2026-10-21T00:00:00Z"), List.of("feed=url"), null, null);

        assertEquals("mint-1", event.tokenAddress());
        assertEquals(List.of("feed=url"), event.evidence());
        assertEquals(SocialEventStatus.PUBLISHED, event.status());
    }
}
