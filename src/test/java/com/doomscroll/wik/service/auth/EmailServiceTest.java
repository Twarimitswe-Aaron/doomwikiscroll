package com.doomscroll.wik.service.auth;

import com.doomscroll.wik.dto.RedisEmailOutbox;
import com.doomscroll.wik.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private EmailService emailService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();
        user.setId(UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@example.com");
    }

    @Test
    void queueVerificationEmail_Success() throws Exception {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        emailService.queueVerificationEmail(user, "http://verification-link");

        verify(hashOperations).put(eq("emails:all"), anyString(), any(RedisEmailOutbox.class));
        verify(zSetOperations).add(eq("emails:pending"), anyString(), anyDouble());
    }

    @Test
    @SuppressWarnings("unchecked")
    void processDueEmails_Success() throws Exception {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        String id = UUID.randomUUID().toString();
        when(zSetOperations.rangeByScore(eq("emails:pending"), anyDouble(), anyDouble(), anyLong(), anyLong()))
                .thenReturn(Set.of(id));

        RedisEmailOutbox email = RedisEmailOutbox.builder()
                .id(id)
                .emailType("EMAIL_VERIFICATION")
                .recipientEmail("test@example.com")
                .subject("Test")
                .templateName("template")
                .templateVariables("{}")
                .attempts(0)
                .maxAttempts(5)
                .build();

        when(hashOperations.get("emails:all", id)).thenReturn(email);
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class))).thenReturn(Map.of());
        when(templateEngine.process(anyString(), any())).thenReturn("<html></html>");
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.processDueEmails();

        verify(mailSender).send(any(MimeMessage.class));
        verify(zSetOperations).remove("emails:pending", id);
        verify(hashOperations).delete("emails:all", id);
    }

    @Test
    void cancelUnsentByUserAndType_Success() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        String id = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();
        when(zSetOperations.range("emails:pending", 0, -1)).thenReturn(Set.of(id));

        RedisEmailOutbox email = RedisEmailOutbox.builder()
                .id(id)
                .userId(userId.toString())
                .emailType("EMAIL_VERIFICATION")
                .build();

        when(hashOperations.get("emails:all", id)).thenReturn(email);

        emailService.cancelUnsentByUserAndType(userId, "EMAIL_VERIFICATION");

        verify(zSetOperations).remove("emails:pending", id);
        verify(hashOperations).put(eq("emails:all"), eq(id), any(RedisEmailOutbox.class));
    }
}
