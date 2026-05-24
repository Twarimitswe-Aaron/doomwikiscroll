package com.doomscroll.wik.service.auth;

import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.entity.VerificationToken;
import com.doomscroll.wik.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    @Value("${app.security.password-reset-token-expiration-minutes:60}")
    private int passwordResetTokenExpirationMinutes;

    @Transactional
    public VerificationToken createEmailVerificationToken(User user) {
        // Invalidate any existing verification tokens
        tokenRepository.invalidateTokensByUserAndType(user.getId(), "EMAIL_VERIFICATION");

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(generateSecureToken())
                .tokenType("EMAIL_VERIFICATION")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        return tokenRepository.save(token);
    }

    @Transactional
    public VerificationToken createPasswordResetToken(User user) {
        // Invalidate any existing password reset tokens
        tokenRepository.invalidateTokensByUserAndType(user.getId(), "PASSWORD_RESET");

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(generateSecureToken())
                .tokenType("PASSWORD_RESET")
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetTokenExpirationMinutes))
                .used(false)
                .build();

        return tokenRepository.save(token);
    }

    @Transactional
    public VerificationToken create2FAToken(User user) {
        // Invalidate any existing 2FA tokens
        tokenRepository.invalidateTokensByUserAndType(user.getId(), "TWO_FACTOR");

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(generateSecureToken())
                .tokenType("TWO_FACTOR")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        return tokenRepository.save(token);
    }

    public Optional<VerificationToken> validateToken(String token, String tokenType) {
        Optional<VerificationToken> verificationToken = tokenRepository
                .findByTokenAndTokenType(token, tokenType);

        if (verificationToken.isPresent()) {
            VerificationToken vt = verificationToken.get();

            // Check if token is used
            if (vt.isUsed()) {
                return Optional.empty();
            }

            // Check if token is expired
            if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
                return Optional.empty();
            }

            return Optional.of(vt);
        }

        return Optional.empty();
    }

    @Transactional
    public void markTokenAsUsed(VerificationToken token) {
        token.setUsed(true);
        tokenRepository.save(token);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    public String generate2FACode() {
        // Generate a 6-digit code
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
}
