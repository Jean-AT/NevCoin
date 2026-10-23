package com.trading.nevcoin.social.application.ports;

import com.trading.nevcoin.social.domain.SocialEntity;
import com.trading.nevcoin.social.domain.TokenAssociation;

import java.util.List;

public interface TokenAssociationResolver {
    List<TokenAssociation> resolve(String content, List<SocialEntity> entities, String sourceEventId);
}
