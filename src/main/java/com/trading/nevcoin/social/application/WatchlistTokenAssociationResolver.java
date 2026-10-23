package com.trading.nevcoin.social.application;

import com.trading.nevcoin.discovery.domain.SolanaMintAddress;
import com.trading.nevcoin.market.application.ports.MarketTokenMetadataPort;
import com.trading.nevcoin.market.application.ports.MarketWatchlistPort;
import com.trading.nevcoin.social.application.ports.TokenAssociationResolver;
import com.trading.nevcoin.social.domain.SocialConfidence;
import com.trading.nevcoin.social.domain.SocialEntity;
import com.trading.nevcoin.social.domain.SocialEntityType;
import com.trading.nevcoin.social.domain.SocialEvidence;
import com.trading.nevcoin.social.domain.TokenAssociation;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Resolves only explicit mints or unique watched-token cashtags; names remain ambiguous. */
@Component
public class WatchlistTokenAssociationResolver implements TokenAssociationResolver {
    private static final String VERSION = "association-v1";
    private final MarketWatchlistPort watchlist;
    private final MarketTokenMetadataPort metadata;

    public WatchlistTokenAssociationResolver(MarketWatchlistPort watchlist, MarketTokenMetadataPort metadata) {
        this.watchlist = watchlist;
        this.metadata = metadata;
    }

    @Override
    public List<TokenAssociation> resolve(String content, List<SocialEntity> entities, String sourceEventId) {
        List<TokenAssociation> associations = new ArrayList<>();
        for (SocialEntity entity : entities) {
            if (entity.type() == SocialEntityType.TOKEN_MINT && SolanaMintAddress.isValid(entity.value())) {
                associations.add(new TokenAssociation(entity.value(), "DIRECT_CONTRACT_REFERENCE",
                        SocialConfidence.VERY_HIGH, VERSION,
                        List.of(evidence("TOKEN_MINT", entity.value(), sourceEventId, SocialConfidence.VERY_HIGH))));
                continue;
            }
            if (entity.type() != SocialEntityType.CASHTAG) continue;
            String symbol = entity.value().toLowerCase(Locale.ROOT);
            List<String> matches = watchlist.activeMintAddresses().stream()
                    .filter(mint -> metadata.findByMintAddress(mint)
                            .map(value -> value.symbol() != null && value.symbol().equalsIgnoreCase(symbol))
                            .orElse(false))
                    .toList();
            if (matches.size() == 1) {
                associations.add(new TokenAssociation(matches.getFirst(), "DIRECT_SYMBOL_REFERENCE",
                        SocialConfidence.HIGH, VERSION,
                        List.of(evidence("CASHTAG", entity.value(), sourceEventId, SocialConfidence.HIGH))));
            }
        }
        return associations.stream().distinct().toList();
    }

    private SocialEvidence evidence(String type, String value, String sourceEventId, SocialConfidence confidence) {
        return new SocialEvidence(type, value, sourceEventId, Instant.now(), confidence);
    }
}
