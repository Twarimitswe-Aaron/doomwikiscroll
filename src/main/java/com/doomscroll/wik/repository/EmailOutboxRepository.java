package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.EmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, UUID> {

    @Query(value = """
            UPDATE email_outbox
            SET status = 'SENDING', attempts = attempts + 1
            WHERE id IN (
                SELECT id FROM email_outbox
                WHERE status IN ('PENDING', 'FAILED')
                  AND next_attempt_at <= :now
                  AND attempts < max_attempts
                ORDER BY next_attempt_at ASC, created_at ASC
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            RETURNING *
            """, nativeQuery = true)
    List<EmailOutbox> claimEmails(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Modifying
    @Query("""
            UPDATE EmailOutbox e
            SET e.status = 'FAILED', e.lastError = 'Stuck in SENDING state'
            WHERE e.status = 'SENDING'
              AND e.updatedAt <= :stuckThreshold
            """)
    int recoverStuckEmails(@Param("stuckThreshold") LocalDateTime stuckThreshold);

    @Modifying
    @Query("""
            UPDATE EmailOutbox e
            SET e.status = 'CANCELED'
            WHERE e.user.id = :userId
              AND e.emailType = :emailType
              AND e.status IN ('PENDING', 'FAILED')
            """)
    void cancelUnsentByUserAndType(@Param("userId") UUID userId, @Param("emailType") String emailType);
}
