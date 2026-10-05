package com.trading.nevcoin.market.application.ports;

import java.util.Optional;

public interface MarketTokenMetadataPort {

    Optional<TokenMetadata> findByMintAddress(String mintAddress);

    record TokenMetadata(String mintAddress, String symbol, String name) {
        public String displayName() {
            if (name != null && !name.isBlank() && symbol != null && !symbol.isBlank()
                    && !name.equalsIgnoreCase(symbol)) {
                return name + " (" + symbol + ")";
            }
            if (name != null && !name.isBlank()) {
                return name;
            }
            if (symbol != null && !symbol.isBlank()) {
                return symbol;
            }
            return "Unknown token";
        }
    }
}
