package com.trading.nevcoin.discovery.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenEligibilityScorerTest {

    @Test
    void scoresCandidateUsingConfiguredThresholds() {
        DiscoveryProperties properties = new DiscoveryProperties();
        properties.setMinimumLiquidityUsd(BigDecimal.valueOf(10_000));
        properties.setMinimumVolume24hUsd(BigDecimal.valueOf(25_000));
        properties.setMinimumUniqueTraders5m(10);
        TokenEligibilityScorer scorer = new TokenEligibilityScorer(properties);

        var candidate = scorer.evaluate(new TokenEligibilityScorer.TokenMarketData(
                "Mint", BigDecimal.valueOf(12_000), BigDecimal.valueOf(30_000), 12), Instant.now());

        assertEquals(BigDecimal.valueOf(100).setScale(2), candidate.eligibilityScore());
        assertEquals(3, candidate.reasons().size());
        assertTrue(candidate.expiresAt().isAfter(candidate.observedAt()));
    }

    @Test
    void doesNotPassMissingMarketValues() {
        TokenEligibilityScorer scorer = new TokenEligibilityScorer(new DiscoveryProperties());

        var candidate = scorer.evaluate(new TokenEligibilityScorer.TokenMarketData(
                "Mint", null, null, 0), Instant.now());

        assertEquals(BigDecimal.valueOf(33.33).setScale(2), candidate.eligibilityScore());
        assertEquals(1, candidate.reasons().size());
    }
}
