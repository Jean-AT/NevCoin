package com.trading.nevcoin.discovery.application;

import com.trading.nevcoin.discovery.application.ports.TokenWatchlistStore;
import com.trading.nevcoin.discovery.domain.Token;
import com.trading.nevcoin.discovery.domain.SolanaMintAddress;
import com.trading.nevcoin.notification.application.ports.TokenQueryPort;
import com.trading.nevcoin.notification.application.ports.TokenWatchlistCommandPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TokenWatchlistService implements TokenQueryPort, TokenWatchlistCommandPort {

    private final TokenWatchlistStore store;

    public TokenWatchlistService(TokenWatchlistStore store) {
        this.store = store;
    }

    @Transactional
    public Token watch(String mintAddress, String symbol, String name) {
        String normalizedMint = requireMint(mintAddress);
        Token existing = store.findByMintAddress(normalizedMint).orElse(null);
        if (existing != null) {
            return store.save(new Token(
                    existing.mintAddress(),
                    firstPresent(symbol, existing.symbol()),
                    firstPresent(name, existing.name()),
                    existing.decimals(),
                    existing.createdAt(),
                    existing.firstSeenAt(),
                    Token.Status.ACTIVE));
        }
        return store.save(new Token(normalizedMint, blankAsNull(symbol), blankAsNull(name), 0,
                null, Instant.now(), Token.Status.ACTIVE));
    }

    @Override
    public void watch(String mintAddress) { watch(mintAddress, null, null); }

    @Override
    @Transactional
    public void updateMetadata(String mintAddress, String symbol, String name) {
        String normalizedMint = requireMint(mintAddress);
        store.findByMintAddress(normalizedMint).ifPresent(existing -> store.save(new Token(
                existing.mintAddress(),
                firstPresent(symbol, existing.symbol()),
                firstPresent(name, existing.name()),
                existing.decimals(),
                existing.createdAt(),
                existing.firstSeenAt(),
                existing.status())));
    }

    @Transactional
    public void unwatch(String mintAddress) {
        store.delete(requireStoredMint(mintAddress));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenSummary> list() {
        return store.findAll().stream().map(this::summary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TokenSummary find(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return store.findByMintAddress(query.trim())
                .or(() -> store.findBySymbol(query.trim()))
                .map(this::summary)
                .orElse(null);
    }

    private TokenSummary summary(Token token) {
        return new TokenSummary(token.mintAddress(), token.symbol(), token.name(), token.status().name(), token.firstSeenAt());
    }

    private String requireMint(String value) {
        return SolanaMintAddress.requireValid(value);
    }

    private String requireStoredMint(String value) {
        if (value == null || value.isBlank() || value.length() > 100 || value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("A stored token mint address is required");
        }
        return value.trim();
    }

    private String blankAsNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String firstPresent(String preferred, String fallback) {
        String normalized = blankAsNull(preferred);
        return normalized == null ? fallback : normalized;
    }
}
