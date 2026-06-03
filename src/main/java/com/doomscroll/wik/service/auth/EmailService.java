package com.doomscroll.wik.service.auth;

import com.doomscroll.wik.entity.EmailOutbox;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.repository.EmailOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final TypeReference<Map<String, Object>> TEMPLATE_VARIABLES_TYPE = new TypeReference<>() {
    };

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final EmailOutboxRepository emailOutboxRepository;
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
        var dueEmails = emailOutboxRepository.findDueEmails(LocalDateTime.now(), PageRequest.of(0, batchSize));
        for (EmailOutbox email : dueEmails) {
            processEmail(email);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEmail(EmailOutbox email) {
        email.setStatus("SENDING");
        email.setAttempts(email.getAttempts() + 1);
        emailOutboxRepository.saveAndFlush(email);

        try {
            sendEmail(email);
            email.setStatus("SENT");
            email.setSentAt(LocalDateTime.now());
            email.setLastError(null);
            log.info("Email {} sent to {}", email.getEmailType(), email.getRecipientEmail());
        } catch (Exception e) {
            email.setLastError(ExceptionUtils.getRootCauseMessage(e));
            if (email.getAttempts() >= email.getMaxAttempts()) {
                email.setStatus("DEAD");
                log.error("Email {} to {} permanently failed after {} attempts",
                        email.getEmailType(), email.getRecipientEmail(), email.getAttempts(), e);
            } else {
                email.setStatus("FAILED");
                email.setNextAttemptAt(LocalDateTime.now().plusSeconds(retryDelaySeconds));
                log.warn("Email {} to {} failed; retry scheduled at {}",
                        email.getEmailType(), email.getRecipientEmail(), email.getNextAttemptAt(), e);
            }
        }

        emailOutboxRepository.save(email);
    }

    private void queueCriticalUserEmail(
            User user,
            String emailType,
            String subject,
            String templateName,
            Map<String, Object> templateVariables
    ) {
        emailOutboxRepository.cancelUnsentByUserAndType(user.getId(), emailType);
        queueEmail(user, emailType, user.getEmail(), subject, templateName, templateVariables);
    }

    private void queueEmail(
            User user,
            String emailType,
            String recipientEmail,
            String subject,
            String templateName,
            Map<String, Object> templateVariables
    ) {
        EmailOutbox email = EmailOutbox.builder()
                .user(user)
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

        emailOutboxRepository.save(email);
        log.info("Queued {} email to {}", emailType, recipientEmail);
    }

    private void sendEmail(EmailOutbox email) throws MessagingException, JsonProcessingException {
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
