package com.trading.nevcoin.discovery.infrastructure.market;

import com.trading.nevcoin.discovery.application.ports.TokenWatchlistStore;
import com.trading.nevcoin.discovery.domain.Token;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistedMarketWatchlistAdapterTest {

    private static final String SOLANA_MINT = "oreoU2P8bN6jkk3jbaiVxYnG1dCXcYxwhwyK9jSybcp";

    @Test
    void returnsOnlyActiveValidSolanaMints() {
        Token active = token(SOLANA_MINT, Token.Status.ACTIVE);
        Token paused = token("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", Token.Status.PAUSED);
        Token legacyEvm = token("0x570a5d26f7765ecb712c0924e4de545b89fd43df", Token.Status.ACTIVE);
        PersistedMarketWatchlistAdapter adapter = new PersistedMarketWatchlistAdapter(
                new FixedStore(List.of(active, paused, legacyEvm)));

        assertEquals(java.util.Set.of(SOLANA_MINT), adapter.activeMintAddresses());
    }

    private Token token(String mint, Token.Status status) {
        return new Token(mint, null, null, 0, null, Instant.now(), status);
    }

    private record FixedStore(List<Token> tokens) implements TokenWatchlistStore {
        @Override public Token save(Token token) { throw new UnsupportedOperationException(); }
        @Override public void delete(String mintAddress) { throw new UnsupportedOperationException(); }
        @Override public List<Token> findAll() { return tokens; }
        @Override public Optional<Token> findByMintAddress(String mintAddress) { return Optional.empty(); }
        @Override public Optional<Token> findBySymbol(String symbol) { return Optional.empty(); }
    }
}
