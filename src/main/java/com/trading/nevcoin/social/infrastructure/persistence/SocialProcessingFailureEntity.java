package com.trading.nevcoin.social.infrastructure.persistence;

import com.trading.nevcoin.social.application.ports.SocialProviderFailure;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "social_processing_failures")
public class SocialProcessingFailureEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sourceId;
    private String stage;
    private String category;
    private String message;
    private boolean retryable;
    private Instant failedAt;
    private int attemptCount;

    protected SocialProcessingFailureEntity() { }

    public SocialProcessingFailureEntity(String sourceId, String stage, SocialProviderFailure failure, Instant failedAt) {
        this.sourceId = sourceId;
        this.stage = stage;
        this.category = failure.category();
        this.message = failure.message();
        this.retryable = failure.retryable();
        this.failedAt = failedAt;
        this.attemptCount = 1;
    }
}
