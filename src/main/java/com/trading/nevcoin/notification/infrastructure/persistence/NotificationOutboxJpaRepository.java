package com.trading.nevcoin.notification.infrastructure.persistence;

import com.trading.nevcoin.notification.domain.NotificationOutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface NotificationOutboxJpaRepository extends JpaRepository<NotificationOutboxEntity, String> {
    Optional<NotificationOutboxEntity> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(value = "INSERT INTO notification_outbox "
            + "(id, idempotency_key, event_type, channel, recipient, payload, status, attempts, next_attempt_at, created_at, updated_at) "
            + "VALUES (:id, :idempotencyKey, :eventType, :channel, :recipient, :payload, 'PENDING', 0, :now, :now, :now) "
            + "ON CONFLICT (idempotency_key) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("idempotencyKey") String idempotencyKey,
                       @Param("eventType") String eventType,
                       @Param("channel") String channel, @Param("recipient") String recipient,
                       @Param("payload") String payload, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entry from NotificationOutboxEntity entry "
            + "where entry.status in :statuses and entry.nextAttemptAt <= :now "
            + "and (entry.claimedUntil is null or entry.claimedUntil <= :now) "
            + "order by entry.nextAttemptAt asc, entry.createdAt asc")
    List<NotificationOutboxEntity> findClaimable(@Param("statuses") List<NotificationOutboxStatus> statuses,
                                                  @Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("update NotificationOutboxEntity entry set entry.status = :status, entry.lastError = :error, "
            + "entry.nextAttemptAt = :nextAttemptAt, entry.claimToken = null, entry.claimedUntil = null, "
            + "entry.updatedAt = :updatedAt where entry.id = :id and entry.claimToken = :claimToken")
    int markFailed(@Param("id") String id, @Param("claimToken") String claimToken,
                   @Param("status") NotificationOutboxStatus status, @Param("error") String error,
                   @Param("nextAttemptAt") Instant nextAttemptAt, @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Query("update NotificationOutboxEntity entry set entry.status = :status, entry.deliveredAt = :deliveredAt, "
            + "entry.claimToken = null, entry.claimedUntil = null, entry.updatedAt = :updatedAt "
            + "where entry.id = :id and entry.claimToken = :claimToken")
    int markDelivered(@Param("id") String id, @Param("claimToken") String claimToken,
                      @Param("status") NotificationOutboxStatus status, @Param("deliveredAt") Instant deliveredAt,
                      @Param("updatedAt") Instant updatedAt);
}
