package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.EmailOutbox;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT e FROM EmailOutbox e
            WHERE e.status IN ('PENDING', 'FAILED')
              AND e.nextAttemptAt <= :now
              AND e.attempts < e.maxAttempts
            ORDER BY e.nextAttemptAt ASC, e.createdAt ASC
            """)
    List<EmailOutbox> findDueEmails(@Param("now") LocalDateTime now, Pageable pageable);

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
