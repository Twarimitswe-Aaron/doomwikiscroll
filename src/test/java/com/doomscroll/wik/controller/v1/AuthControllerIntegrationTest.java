package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.dto.request.auth.ForgotPasswordRequest;
import com.doomscroll.wik.dto.request.auth.LoginRequest;
import com.doomscroll.wik.dto.request.auth.RegisterRequest;
import com.doomscroll.wik.dto.request.auth.ResetPasswordRequest;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.entity.VerificationToken;
import com.doomscroll.wik.entity.enums.UserRole;
import com.doomscroll.wik.entity.enums.UserStatus;
import com.doomscroll.wik.repository.UserRepository;
import com.doomscroll.wik.repository.VerificationTokenRepository;
import com.doomscroll.wik.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// NOTE: @Transactional is intentionally omitted here. MockMvc dispatches requests
// in a separate thread, so Spring's test-transaction does NOT roll back after each
// test. @BeforeEach cleanup is the correct pattern for MockMvc integration tests.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // Stubs out the mail sender so no SMTP calls escape during tests.
    // The outbox scheduler should also be disabled via application-test.yml
    // (e.g., spring.task.scheduling.enabled=false).
    @MockBean
    private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        // Delete child FK rows first, then parent to satisfy referential integrity.
        // If new FK-dependent entities are added, clean them up here before
        // calling verificationTokenRepository / userRepository.deleteAll().
        verificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User createVerifiedUser() {
        return userRepository.save(User.builder()
                .username("verifieduser")
                .email("verified@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .accountLocked(false)
                .loginAttempts(0)
                .build());
    }

    private User createUnverifiedUser() {
        return userRepository.save(User.builder()
                .username("unverifieduser")
                .email("unverified@example.com")
                .passwordHash(passwordEncoder.encode("Test@123"))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .accountLocked(false)
                .loginAttempts(0)
                .build());
    }

    // ── Registration ─────────────────────────────────────────────────────────

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("Test@123");
        request.setFirstName("Test");
        request.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void register_InvalidPassword() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("weak"); // Too weak password
        request.setFirstName("Test");
        request.setLastName("User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_Success() throws Exception {
        createVerifiedUser();

        LoginRequest request = new LoginRequest();
        request.setEmail("verified@example.com");
        request.setPassword("Test@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void login_UserNotFound() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WrongPassword() throws Exception {
        // User exists, but password is incorrect.
        createVerifiedUser();

        LoginRequest request = new LoginRequest();
        request.setEmail("verified@example.com");
        request.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_UnverifiedEmail_IsRejected() throws Exception {
        createUnverifiedUser();

        LoginRequest request = new LoginRequest();
        request.setEmail("unverified@example.com");
        request.setPassword("Test@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── Email Verification ────────────────────────────────────────────────────

    @Test
    void verifyEmail_Success() throws Exception {
        User user = createUnverifiedUser();

        VerificationToken token = verificationTokenRepository.save(VerificationToken.builder()
                .token("valid-verification-token")
                .tokenType("EMAIL_VERIFICATION")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .used(false)
                .build());

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    void verifyEmail_ExpiredToken_IsRejected() throws Exception {
        User user = createUnverifiedUser();

        verificationTokenRepository.save(VerificationToken.builder()
                .token("expired-verification-token")
                .tokenType("EMAIL_VERIFICATION")
                .user(user)
                .expiresAt(LocalDateTime.now().minusHours(1)) // already expired
                .used(false)
                .build());

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "expired-verification-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmail_AlreadyUsedToken_IsRejected() throws Exception {
        User user = createUnverifiedUser();

        verificationTokenRepository.save(VerificationToken.builder()
                .token("used-verification-token")
                .tokenType("EMAIL_VERIFICATION")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .used(true) // already consumed
                .build());

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .param("token", "used-verification-token"))
                .andExpect(status().isBadRequest());
    }

    // ── Forgot / Reset Password ───────────────────────────────────────────────

    @Test
    void forgotPassword_Success() throws Exception {
        createVerifiedUser();
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("verified@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void resetPassword_Success() throws Exception {
        User user = createVerifiedUser();
        VerificationToken token = verificationTokenRepository.save(VerificationToken.builder()
                .token("valid-reset-token")
                .tokenType("PASSWORD_RESET")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token.getToken());
        request.setNewPassword("NewTest@123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }

    @Test
    void resetPassword_ExpiredToken_IsRejected() throws Exception {
        User user = createVerifiedUser();
        verificationTokenRepository.save(VerificationToken.builder()
                .token("expired-reset-token")
                .tokenType("PASSWORD_RESET")
                .user(user)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // expired
                .used(false)
                .build());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-reset-token");
        request.setNewPassword("NewTest@123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── Token Refresh ─────────────────────────────────────────────────────────

    @Test
    void refreshToken_Success() throws Exception {
        User user = createVerifiedUser();
        String token = jwtService.generateRefreshToken(user);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }
}
