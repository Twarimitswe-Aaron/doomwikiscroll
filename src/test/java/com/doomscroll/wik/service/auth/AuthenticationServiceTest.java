package com.doomscroll.wik.service.auth;

import com.doomscroll.wik.dto.request.auth.LoginRequest;
import com.doomscroll.wik.dto.request.auth.RegisterRequest;
import com.doomscroll.wik.dto.response.auth.AuthenticationResponse;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.entity.VerificationToken;
import com.doomscroll.wik.entity.enums.UserRole;
import com.doomscroll.wik.entity.enums.UserStatus;
import com.doomscroll.wik.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Test@123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Test@123");

        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .accountLocked(false)
                .loginAttempts(0)
                .build();
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        VerificationToken vt = VerificationToken.builder()
                .token("verificationToken")
                .tokenType("EMAIL_VERIFICATION")
                .user(user)
                .build();
        when(verificationTokenService.createEmailVerificationToken(any(User.class))).thenReturn(vt);

        AuthenticationResponse response = authenticationService.register(registerRequest);

        assertNotNull(response);
        assertNull(response.getAccessToken());
        assertNull(response.getRefreshToken());
        assertEquals(user.getId(), response.getUserId());
        assertEquals(user.getDisplayUsername(), response.getUsername());
        assertEquals(user.getEmail(), response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_EmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                authenticationService.register(registerRequest));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        user.setEmailVerified(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refreshToken");

        AuthenticationResponse response = authenticationService.login(loginRequest, "127.0.0.1");

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).resetLoginAttempts(anyString());
    }
}
