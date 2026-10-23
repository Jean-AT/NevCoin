package com.trading.nevcoin.social.application;

import com.trading.nevcoin.social.application.ports.SocialEntityExtractor;
import com.trading.nevcoin.social.domain.SocialEntity;
import com.trading.nevcoin.social.domain.SocialEntityType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic extraction of explicit, auditable social entities. */
@Component
public class RuleBasedSocialEntityExtractor implements SocialEntityExtractor {
    private static final Pattern MINT = Pattern.compile("(?<![1-9A-HJ-NP-Za-km-z])[1-9A-HJ-NP-Za-km-z]{32,44}(?![1-9A-HJ-NP-Za-km-z])");
    private static final Pattern CASHTAG = Pattern.compile("\\$([A-Za-z][A-Za-z0-9_]{1,14})\\b");
    private static final Pattern HASHTAG = Pattern.compile("#([\\p{L}][\\p{L}0-9_]{1,63})");

    @Override
    public List<SocialEntity> extract(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<SocialEntity> entities = new ArrayList<>();
        collect(content, MINT, SocialEntityType.TOKEN_MINT, entities, false);
        collect(content, CASHTAG, SocialEntityType.CASHTAG, entities, true);
        collect(content, HASHTAG, SocialEntityType.HASHTAG, entities, true);
        return entities.stream().distinct().sorted(java.util.Comparator.comparingInt(SocialEntity::startOffset)).toList();
    }

    private static void collect(String content, Pattern pattern, SocialEntityType type,
                                List<SocialEntity> target, boolean useGroup) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            int start = useGroup ? matcher.start(1) : matcher.start();
            int end = useGroup ? matcher.end(1) : matcher.end();
            target.add(new SocialEntity(type, content.substring(start, end), start, end));
        }
    }
}
