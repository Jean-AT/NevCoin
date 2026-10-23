package com.trading.nevcoin.social.application;

import com.trading.nevcoin.market.application.ports.MarketTokenMetadataPort;
import com.trading.nevcoin.market.application.ports.MarketWatchlistPort;
import com.trading.nevcoin.social.domain.SocialConfidence;
import com.trading.nevcoin.social.domain.SocialEntityType;
import com.trading.nevcoin.social.domain.TokenAssociation;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialEntityExtractionTest {

    @Test
    void extractsMintCashtagAndHashtagWithOffsets() {
        String mint = "So11111111111111111111111111111111111111112";
        var entities = new RuleBasedSocialEntityExtractor().extract("Buy $ORE and #sol, mint " + mint);

        assertEquals(3, entities.size());
        assertEquals(SocialEntityType.CASHTAG, entities.getFirst().type());
        assertEquals("ORE", entities.getFirst().value());
        assertEquals(SocialEntityType.TOKEN_MINT, entities.get(2).type());
        assertEquals(mint, entities.get(2).value());
        assertEquals(mint, ("Buy $ORE and #sol, mint " + mint)
                .substring(entities.get(2).startOffset(), entities.get(2).endOffset()));
    }

    @Test
    void ignoresAmbiguousSymbolAndAcceptsUniqueWatchedSymbol() {
        var watchlist = (MarketWatchlistPort) () -> Set.of("MintA", "MintB");
        var metadata = (MarketTokenMetadataPort) mint -> switch (mint) {
            case "MintA" -> Optional.of(new MarketTokenMetadataPort.TokenMetadata(mint, "ORE", "Ore"));
            case "MintB" -> Optional.of(new MarketTokenMetadataPort.TokenMetadata(mint, "ORE", "Ore 2"));
            default -> Optional.empty();
        };
        var resolver = new WatchlistTokenAssociationResolver(watchlist, metadata);
        var extractor = new RuleBasedSocialEntityExtractor();

        assertTrue(resolver.resolve("$ORE", extractor.extract("$ORE"), "event-1").isEmpty());

        var uniqueResolver = new WatchlistTokenAssociationResolver(
                () -> Set.of("MintA"), metadata);
        var associations = uniqueResolver.resolve("$ORE", extractor.extract("$ORE"), "event-2");
        assertEquals(1, associations.size());
        TokenAssociation association = associations.getFirst();
        assertEquals("MintA", association.tokenAddress());
        assertEquals("DIRECT_SYMBOL_REFERENCE", association.associationType());
        assertEquals(SocialConfidence.HIGH, association.confidence());
        assertEquals("CASHTAG", association.evidence().getFirst().type());
    }
}
