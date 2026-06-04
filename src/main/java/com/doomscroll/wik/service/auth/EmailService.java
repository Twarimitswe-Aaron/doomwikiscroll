package com.doomscroll.wik.service.auth;

import com.doomscroll.wik.dto.RedisEmailOutbox;
import com.doomscroll.wik.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final TypeReference<Map<String, Object>> TEMPLATE_VARIABLES_TYPE = new TypeReference<>() {
    };

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.outbox.batch-size:25}")
    private int batchSize;

    @Value("${app.email.outbox.retry-delay-seconds:60}")
    private int retryDelaySeconds;

    @Transactional
    public void queueVerificationEmail(User user, String verificationLink) {
        queueCriticalUserEmail(
                user,
                "EMAIL_VERIFICATION",
                "Verify your email - Wiki History",
                "email/verification",
                Map.of(
                        "username", user.getDisplayUsername(),
                        "verificationLink", verificationLink,
                        "appName", "Wiki History"
                )
        );
    }

    @Transactional
    public void queuePasswordResetEmail(User user, String resetLink) {
        queueCriticalUserEmail(
                user,
                "PASSWORD_RESET",
                "Reset your password - Wiki History",
                "email/password-reset",
                Map.of(
                        "username", user.getDisplayUsername(),
                        "resetLink", resetLink,
                        "expiryTime", "1 hour",
                        "appName", "Wiki History"
                )
        );
    }

    @Transactional
    public void queue2FACode(User user, String code) {
        queueCriticalUserEmail(
                user,
                "TWO_FACTOR",
                "Your 2FA Code - Wiki History",
                "email/2fa-code",
                Map.of(
                        "username", user.getDisplayUsername(),
                        "code", code,
                        "appName", "Wiki History"
                )
        );
    }

    @Transactional
    public void queueWelcomeEmail(User user) {
        queueEmail(
                user,
                "WELCOME",
                user.getEmail(),
                "Welcome to Wiki History!",
                "email/welcome",
                Map.of(
                        "username", user.getDisplayUsername(),
                        "appName", "Wiki History",
                        "loginLink", "http://localhost:3000/login"
                )
        );
    }

    @Transactional
    public void processDueEmails() {
        long nowMillis = LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        // Fetch IDs that are due (score <= nowMillis) up to batchSize limit
        Set<Object> dueIds = redisTemplate.opsForZSet().rangeByScore("emails:pending", 0, nowMillis, 0, batchSize);
        if (dueIds == null || dueIds.isEmpty()) return;

        for (Object idObj : dueIds) {
            String id = (String) idObj;
            RedisEmailOutbox email = (RedisEmailOutbox) redisTemplate.opsForHash().get("emails:all", id);
            if (email != null) {
                processEmail(email);
            } else {
                // If not found in hash, clean it up from the ZSET
                redisTemplate.opsForZSet().remove("emails:pending", id);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEmail(RedisEmailOutbox email) {
        String id = email.getId();
        email.setAttempts(email.getAttempts() + 1);

        try {
            sendEmail(email);
            email.setStatus("SENT");
            email.setSentAt(LocalDateTime.now());
            email.setLastError(null);
            
            // Remove from pending queue since it's successfully sent
            redisTemplate.opsForZSet().remove("emails:pending", id);
            // Delete from all hash to keep Redis clean
            redisTemplate.opsForHash().delete("emails:all", id);
            
            log.info("Email {} sent to {}", email.getEmailType(), email.getRecipientEmail());
        } catch (Exception e) {
            String errorMsg = ExceptionUtils.getRootCauseMessage(e);
            email.setLastError(errorMsg);
            
            if (email.getAttempts() >= email.getMaxAttempts()) {
                email.setStatus("DEAD");
                // Remove from pending queue permanently
                redisTemplate.opsForZSet().remove("emails:pending", id);
                // Save updated status in all hash
                redisTemplate.opsForHash().put("emails:all", id, email);
                
                log.error("Email {} to {} permanently failed after {} attempts",
                        email.getEmailType(), email.getRecipientEmail(), email.getAttempts(), e);
            } else {
                email.setStatus("FAILED");
                email.setNextAttemptAt(LocalDateTime.now().plusSeconds(retryDelaySeconds));
                
                // Update score in pending queue to the new nextAttemptAt
                long newScore = email.getNextAttemptAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                redisTemplate.opsForZSet().add("emails:pending", id, newScore);
                // Save updated state in hash
                redisTemplate.opsForHash().put("emails:all", id, email);
                
                log.warn("Email {} to {} failed; retry scheduled at {}",
                        email.getEmailType(), email.getRecipientEmail(), email.getNextAttemptAt(), e);
            }
        }
    }

    private void queueCriticalUserEmail(
            User user,
            String emailType,
            String subject,
            String templateName,
            Map<String, Object> templateVariables
    ) {
        cancelUnsentByUserAndType(user.getId(), emailType);
        queueEmail(user, emailType, user.getEmail(), subject, templateName, templateVariables);
    }

    public void cancelUnsentByUserAndType(UUID userId, String emailType) {
        if (userId == null) return;
        String userIdStr = userId.toString();
        
        Set<Object> pendingIds = redisTemplate.opsForZSet().range("emails:pending", 0, -1);
        if (pendingIds == null || pendingIds.isEmpty()) return;

        for (Object idObj : pendingIds) {
            String id = (String) idObj;
            RedisEmailOutbox email = (RedisEmailOutbox) redisTemplate.opsForHash().get("emails:all", id);
            if (email != null && userIdStr.equals(email.getUserId()) && emailType.equals(email.getEmailType())) {
                redisTemplate.opsForZSet().remove("emails:pending", id);
                email.setStatus("CANCELED");
                redisTemplate.opsForHash().put("emails:all", id, email);
            }
        }
    }

    private void queueEmail(
            User user,
            String emailType,
            String recipientEmail,
            String subject,
            String templateName,
            Map<String, Object> templateVariables
    ) {
        String id = UUID.randomUUID().toString();
        RedisEmailOutbox email = RedisEmailOutbox.builder()
                .id(id)
                .userId(user != null ? user.getId().toString() : null)
                .emailType(emailType)
                .recipientEmail(recipientEmail)
                .subject(subject)
                .templateName(templateName)
                .templateVariables(toJson(templateVariables))
                .status("PENDING")
                .attempts(0)
                .maxAttempts(5)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        redisTemplate.opsForHash().put("emails:all", id, email);

        long score = email.getNextAttemptAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        redisTemplate.opsForZSet().add("emails:pending", id, score);

        log.info("Queued {} email to {}", emailType, recipientEmail);
    }

    private void sendEmail(RedisEmailOutbox email) throws MessagingException, JsonProcessingException {
        Context context = new Context();
        context.setVariables(fromJson(email.getTemplateVariables()));

        String htmlContent = templateEngine.process(email.getTemplateName(), context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(email.getRecipientEmail());
        helper.setSubject(email.getSubject());
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private String toJson(Map<String, Object> templateVariables) {
        try {
            return objectMapper.writeValueAsString(templateVariables);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize email template variables", e);
        }
    }

    private Map<String, Object> fromJson(String templateVariables) throws JsonProcessingException {
        return objectMapper.readValue(templateVariables, TEMPLATE_VARIABLES_TYPE);
    }
}
