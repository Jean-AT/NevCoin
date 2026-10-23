package com.trading.nevcoin.social.application.ports;

import com.trading.nevcoin.social.domain.SocialEntity;

import java.util.List;

public interface SocialEntityExtractor {
    List<SocialEntity> extract(String content);
}
