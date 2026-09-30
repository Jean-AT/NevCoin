package com.trading.nevcoin.discovery.application.ports;

import com.trading.nevcoin.discovery.domain.Token;

import java.util.List;
import java.util.Optional;

public interface TokenWatchlistStore {
    Token save(Token token);
    void delete(String mintAddress);
    List<Token> findAll();
    Optional<Token> findByMintAddress(String mintAddress);
    Optional<Token> findBySymbol(String symbol);
}
