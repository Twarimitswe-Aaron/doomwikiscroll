package com.doomscroll.wik.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_outbox", indexes = {
        @Index(name = "idx_email_outbox_status_next_attempt", columnList = "status,next_attempt_at"),
        @Index(name = "idx_email_outbox_user_type", columnList = "user_id,email_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOutbox extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "email_type", nullable = false, length = 50)
    private String emailType;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "template_variables", nullable = false, columnDefinition = "TEXT")
    private String templateVariables;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Builder.Default
    @Column(nullable = false)
    private Integer attempts = 0;

    @Builder.Default
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 5;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @PrePersist
    void setDefaults() {
        if (nextAttemptAt == null) {
            nextAttemptAt = LocalDateTime.now();
        }
    }
}
