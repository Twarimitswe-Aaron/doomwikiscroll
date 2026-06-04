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
        try {
            log.debug("Processing email outbox");
            emailService.processDueEmails();
        } catch (Exception e) {
            log.error("Error processing email outbox", e);
        }
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void recoverStuckEmails() {
        // Redis-based outbox does not require database stuck email recovery.
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            log.info("Running scheduled cleanup of expired tokens");
            tokenRepository.deleteExpiredTokens();
            log.info("Completed cleanup of expired tokens");
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens", e);
        }
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void logSystemHealth() {
        try {
            log.debug("System health check - running");
        } catch (Exception e) {
            log.error("Error running system health check", e);
        }
    }
}
