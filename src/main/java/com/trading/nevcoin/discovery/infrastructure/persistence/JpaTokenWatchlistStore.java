package com.trading.nevcoin.discovery.infrastructure.persistence;

import com.trading.nevcoin.discovery.application.ports.TokenWatchlistStore;
import com.trading.nevcoin.discovery.domain.Token;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaTokenWatchlistStore implements TokenWatchlistStore {

    private final TokenJpaRepository repository;

    public JpaTokenWatchlistStore(TokenJpaRepository repository) { this.repository = repository; }

    @Override public Token save(Token token) { return repository.save(new TokenEntity(token)).toDomain(); }
    @Override public void delete(String mintAddress) { repository.deleteById(mintAddress); }
    @Override public List<Token> findAll() { return repository.findAll().stream().map(TokenEntity::toDomain).toList(); }
    @Override public Optional<Token> findByMintAddress(String mintAddress) { return repository.findById(mintAddress).map(TokenEntity::toDomain); }
    @Override public Optional<Token> findBySymbol(String symbol) { return repository.findBySymbolIgnoreCase(symbol).map(TokenEntity::toDomain); }
}
