package com.trading.nevcoin.discovery.infrastructure.market;

import com.trading.nevcoin.discovery.application.ports.TokenWatchlistStore;
import com.trading.nevcoin.discovery.domain.Token;
import com.trading.nevcoin.discovery.domain.SolanaMintAddress;
import com.trading.nevcoin.market.application.ports.MarketWatchlistPort;
import com.trading.nevcoin.market.application.ports.MarketTokenMetadataPort;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PersistedMarketWatchlistAdapter implements MarketWatchlistPort, MarketTokenMetadataPort {

    private final TokenWatchlistStore store;

    public PersistedMarketWatchlistAdapter(TokenWatchlistStore store) {
        this.store = store;
    }

    @Override
    public Set<String> activeMintAddresses() {
        return store.findAll().stream()
                .filter(token -> token.status() == Token.Status.ACTIVE)
                .map(Token::mintAddress)
                .filter(SolanaMintAddress::isValid)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public java.util.Optional<TokenMetadata> findByMintAddress(String mintAddress) {
        return store.findByMintAddress(mintAddress)
                .map(token -> new TokenMetadata(token.mintAddress(), token.symbol(), token.name()));
    }
}
