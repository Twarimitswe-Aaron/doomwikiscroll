package com.doomscroll.wik.service.auth;

import com.doomscroll.wik.dto.request.auth.*;
import com.doomscroll.wik.dto.response.auth.AuthenticationResponse;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.entity.VerificationToken;
import com.doomscroll.wik.entity.enums.UserRole;
import com.doomscroll.wik.entity.enums.UserStatus;
import com.doomscroll.wik.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .accountLocked(false)
                .loginAttempts(0)
                .build();

        user = userRepository.save(user);

        // Create verification token
        VerificationToken verificationToken = verificationTokenService.createEmailVerificationToken(user);

        // Queue verification email for scheduled delivery/retry.
        String verificationLink = baseUrl + "/verify-email?token=" + verificationToken.getToken();
        emailService.queueVerificationEmail(user, verificationLink);

        return AuthenticationResponse.builder()
                .userId(user.getId())
                .username(user.getDisplayUsername())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        Optional<VerificationToken> verificationToken =
                verificationTokenService.validateToken(token, "EMAIL_VERIFICATION");

        if (verificationToken.isEmpty()) {
            throw new RuntimeException("Invalid or expired verification token");
        }

        VerificationToken vt = verificationToken.get();
        User user = vt.getUser();

        user.setEmailVerified(true);
        userRepository.save(user);

        verificationTokenService.markTokenAsUsed(vt);

        emailService.queueWelcomeEmail(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

    @Transactional
    public void forgotPassword(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = optionalUser.get();
        VerificationToken token = verificationTokenService.createPasswordResetToken(user);

        String resetLink = baseUrl + "/reset-password?token=" + token.getToken();
        emailService.queuePasswordResetEmail(user, resetLink);

        log.info("Password reset email sent to: {}", email);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            log.info("Resend verification requested for non-existent email: {}", email);
            return;
        }

        User user = optionalUser.get();
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.info("Resend verification requested for already verified email: {}", email);
            return;
        }

        VerificationToken verificationToken = verificationTokenService.createEmailVerificationToken(user);
        String verificationLink = baseUrl + "/verify-email?token=" + verificationToken.getToken();
        emailService.queueVerificationEmail(user, verificationLink);

        log.info("Verification email re-queued for user: {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public void verifyResetToken(String token) {
        Optional<VerificationToken> verificationToken =
                verificationTokenService.validateToken(token, "PASSWORD_RESET");

        if (verificationToken.isEmpty()) {
            throw new RuntimeException("Invalid or expired reset token");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Optional<VerificationToken> verificationToken =
                verificationTokenService.validateToken(request.getToken(), "PASSWORD_RESET");

        if (verificationToken.isEmpty()) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        VerificationToken vt = verificationToken.get();
        User user = vt.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verificationTokenService.markTokenAsUsed(vt);

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    @Transactional
    public AuthenticationResponse login(LoginRequest request, String ipAddress) {
        // Check if user exists
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check account status
        if (user.getAccountLocked()) {
            throw new RuntimeException("Account is locked. Please try again later or reset your password.");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Account is not active. Please contact support.");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Email address is not verified. Please check your inbox or request a new verification email.");
        }

        // Authenticate
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            userRepository.incrementLoginAttempts(request.getEmail());
            throw new BadCredentialsException("Invalid credentials");
        }

        // Reset login attempts on successful login
        userRepository.resetLoginAttempts(request.getEmail());
        userRepository.updateLastLogin(request.getEmail(), LocalDateTime.now(), ipAddress);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getDisplayUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .emailVerified(user.getEmailVerified())
                .build();
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        // Extract username from refresh token
        String userEmail = jwtService.extractUsername(refreshToken);

        if (userEmail == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .username(user.getDisplayUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
