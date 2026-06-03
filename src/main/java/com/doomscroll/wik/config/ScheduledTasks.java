package com.doomscroll.wik.config;

import com.doomscroll.wik.repository.VerificationTokenRepository;
import com.doomscroll.wik.service.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Scheduled(fixedDelayString = "${app.email.outbox.poll-interval-ms:60000}")
    public void processEmailOutbox() {
        log.debug("Processing email outbox");
        emailService.processDueEmails();
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running scheduled cleanup of expired tokens");
        tokenRepository.deleteExpiredTokens();
        log.info("Completed cleanup of expired tokens");
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void logSystemHealth() {
        log.debug("System health check - running");
        // Add more health checks as needed
    }
}
