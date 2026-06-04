package com.doomscroll.wik.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisEmailOutbox implements Serializable {
    private String id;
    private String userId;
    private String emailType;
    private String recipientEmail;
    private String subject;
    private String templateName;
    private String templateVariables;
    private String status;
    @Builder.Default
    private Integer attempts = 0;
    @Builder.Default
    private Integer maxAttempts = 5;
    private LocalDateTime nextAttemptAt;
    private LocalDateTime sentAt;
    private String lastError;
}
