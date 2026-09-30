package com.trading.nevcoin.discovery.application;

import com.trading.nevcoin.discovery.application.ports.TokenWatchlistStore;
import com.trading.nevcoin.discovery.domain.Token;
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
        return store.save(new Token(normalizedMint, blankAsNull(symbol), blankAsNull(name), 0,
                null, Instant.now(), Token.Status.ACTIVE));
    }

    @Override
    public void watch(String mintAddress) { watch(mintAddress, null, null); }

    @Transactional
    public void unwatch(String mintAddress) {
        store.delete(requireMint(mintAddress));
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
        if (value == null || value.isBlank() || value.length() > 100 || value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("A valid token mint address is required");
        }
        return value.trim();
    }

    private String blankAsNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
