package com.trading.nevcoin.social.domain;

public record SocialEntity(
        SocialEntityType type,
        String value,
        int startOffset,
        int endOffset) {
    public SocialEntity {
        if (type == null) throw new IllegalArgumentException("Entity type is required");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Entity value is required");
        if (startOffset < 0 || endOffset < startOffset) {
            throw new IllegalArgumentException("Entity offsets are invalid");
        }
    }
}
