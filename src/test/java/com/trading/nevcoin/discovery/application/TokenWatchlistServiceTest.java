package com.trading.nevcoin.discovery.application;

import com.trading.nevcoin.discovery.application.ports.TokenWatchlistStore;
import com.trading.nevcoin.discovery.domain.Token;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenWatchlistServiceTest {

    private static final String SOLANA_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v";

    private final InMemoryStore store = new InMemoryStore();
    private final TokenWatchlistService service = new TokenWatchlistService(store);

    @Test
    void acceptsSolanaBase58Mint() {
        service.watch(SOLANA_MINT);

        assertEquals(SOLANA_MINT, store.findAll().getFirst().mintAddress());
    }

    @Test
    void rejectsEvmAddress() {
        assertThrows(IllegalArgumentException.class,
                () -> service.watch("0x570a5d26f7765ecb712c0924e4de545b89fd43df"));
    }

    @Test
    void allowsRemovingLegacyInvalidAddress() {
        String legacyAddress = "0x570a5d26f7765ecb712c0924e4de545b89fd43df";
        store.save(new Token(legacyAddress, null, null, 0, null,
                java.time.Instant.now(), Token.Status.ACTIVE));

        service.unwatch(legacyAddress);

        assertEquals(List.of(), store.findAll());
    }

    @Test
    void persistsMetadataWithoutResettingWatchlistState() {
        service.watch(SOLANA_MINT);
        var firstSeenAt = store.findAll().getFirst().firstSeenAt();

        service.updateMetadata(SOLANA_MINT, "USDC", "USD Coin");

        Token enriched = store.findAll().getFirst();
        assertEquals("USDC", enriched.symbol());
        assertEquals("USD Coin", enriched.name());
        assertEquals(firstSeenAt, enriched.firstSeenAt());
    }

    private static class InMemoryStore implements TokenWatchlistStore {
        private final List<Token> tokens = new ArrayList<>();

        @Override public Token save(Token token) {
            tokens.removeIf(existing -> existing.mintAddress().equals(token.mintAddress()));
            tokens.add(token);
            return token;
        }
        @Override public void delete(String mintAddress) { tokens.removeIf(token -> token.mintAddress().equals(mintAddress)); }
        @Override public List<Token> findAll() { return List.copyOf(tokens); }
        @Override public Optional<Token> findByMintAddress(String mintAddress) {
            return tokens.stream().filter(token -> token.mintAddress().equals(mintAddress)).findFirst();
        }
        @Override public Optional<Token> findBySymbol(String symbol) { return Optional.empty(); }
    }
}
