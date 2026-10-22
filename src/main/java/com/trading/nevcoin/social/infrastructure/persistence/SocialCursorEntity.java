package com.trading.nevcoin.social.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "social_provider_cursors")
public class SocialCursorEntity {
    @Id
    @Column(name = "source_id", length = 256)
    private String sourceId;
    @Column(length = 512)
    private String cursor;
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected SocialCursorEntity() { }

    public SocialCursorEntity(String sourceId, String cursor, Instant fetchedAt) {
        this.sourceId = sourceId;
        this.cursor = cursor;
        this.fetchedAt = fetchedAt;
    }

    public String getSourceId() { return sourceId; }
    public String getCursor() { return cursor; }
    public Instant getFetchedAt() { return fetchedAt; }
}
