package com.trading.nevcoin.market.infrastructure.dexscreener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DexScreenerTokenMarketDataAdapterTest {

    private static final String MINT = "mint-address";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DexScreenerTokenMarketDataAdapter adapter = new DexScreenerTokenMarketDataAdapter(
            new DexScreenerProperties(), objectMapper);

    @Test
    void selectsMostLiquidPoolAndMapsMarketMetrics() throws Exception {
        var response = objectMapper.readTree("""
                [
                  {
                    "dexId": "small-dex",
                    "baseToken": {"address": "mint-address", "symbol": "SMALL", "name": "Small Pool"},
                    "priceUsd": "0.01",
                    "liquidity": {"usd": 100}
                  },
                  {
                    "dexId": "raydium",
                    "baseToken": {"address": "mint-address", "symbol": "MEME", "name": "Meme Coin"},
                    "priceUsd": "0.00001234",
                    "marketCap": 850000000,
                    "liquidity": {"usd": 12000000},
                    "volume": {"h24": 340000},
                    "priceChange": {"h24": 5.25},
                    "txns": {"m5": {"buys": 120, "sells": 84}}
                  }
                ]
                """);

        var marketData = adapter.parse(response, MINT).orElseThrow();

        assertEquals("MEME", marketData.symbol());
        assertEquals("Meme Coin", marketData.name());
        assertEquals(new BigDecimal("0.00001234"), marketData.priceUsd());
        assertEquals(new BigDecimal("12000000"), marketData.liquidityUsd());
        assertEquals(120, marketData.buys5m());
        assertEquals(84, marketData.sells5m());
        assertEquals("DEX Screener / raydium", marketData.source());
    }

    @Test
    void ignoresPairsWhereRequestedMintIsNotTheBaseToken() throws Exception {
        var response = objectMapper.readTree("""
                [{"baseToken":{"address":"other"},"quoteToken":{"address":"mint-address"}}]
                """);

        assertTrue(adapter.parse(response, MINT).isEmpty());
    }
}
