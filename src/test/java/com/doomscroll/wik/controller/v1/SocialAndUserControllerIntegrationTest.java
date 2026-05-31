package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.dto.request.comment.CommentRequest;
import com.doomscroll.wik.dto.request.user.UpdateProfileRequest;
import com.doomscroll.wik.dto.request.event.EventRequestDto;
import com.doomscroll.wik.dto.request.InteractionRequest;
import com.doomscroll.wik.entity.Category;
import com.doomscroll.wik.entity.HistoricalEvent;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.entity.enums.UserRole;
import com.doomscroll.wik.entity.enums.UserStatus;
import com.doomscroll.wik.repository.CategoryRepository;
import com.doomscroll.wik.repository.HistoricalEventRepository;
import com.doomscroll.wik.repository.UserRepository;
import com.doomscroll.wik.service.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialAndUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HistoricalEventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private com.doomscroll.wik.repository.CommentRepository commentRepository;

    @Autowired
    private com.doomscroll.wik.repository.BookmarkRepository bookmarkRepository;

    @Autowired
    private com.doomscroll.wik.repository.UserReactionRepository reactionRepository;

    @Autowired
    private com.doomscroll.wik.repository.ReadingHistoryRepository readingHistoryRepository;

    @Autowired
    private com.doomscroll.wik.repository.UserFollowRepository userFollowRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private User adminUser;
    private String userToken;
    private String adminToken;
    private HistoricalEvent testEvent;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        bookmarkRepository.deleteAll();
        reactionRepository.deleteAll();
        readingHistoryRepository.deleteAll();
        userFollowRepository.deleteAll();
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .accountLocked(false)
                .loginAttempts(0)
                .build());

        adminUser = userRepository.save(User.builder()
                .username("adminuser")
                .email("admin@example.com")
                .passwordHash("hashed")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .accountLocked(false)
                .loginAttempts(0)
                .build());

        userToken = jwtService.generateAccessToken(testUser);
        adminToken = jwtService.generateAccessToken(adminUser);

        testCategory = categoryRepository.save(Category.builder()
                .name("History")
                .isActive(true)
                .displayOrder(1)
                .build());

        testEvent = HistoricalEvent.builder()
                .title("Battle of Hastings")
                .summary("A major battle in 1066")
                .detailedContent("Detailed details of Battle of Hastings")
                .source("WIKIPEDIA")
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .shareCount(0L)
                .isTrending(false)
                .isFeatured(false)
                .status("PUBLISHED")
                .build();
        testEvent.setCategories(Set.of(testCategory));
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    void userProfileFlow_Success() throws Exception {
        // GET /me
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.profilePictureUrl").doesNotExist()); // Check strictly omitted

        // PUT /profile
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .bio("Lover of history.")
                .build();

        mockMvc.perform(put("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.bio").value("Lover of history."));
    }

    @Test
    void userBookmarksAndFollows_Success() throws Exception {
        // Toggle bookmark (Add)
        mockMvc.perform(post("/api/v1/users/bookmarks/" + testEvent.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));

        // GET bookmarks
        mockMvc.perform(get("/api/v1/users/bookmarks")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Battle of Hastings"));

        // Toggle bookmark (Remove)
        mockMvc.perform(post("/api/v1/users/bookmarks/" + testEvent.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(false));

        // Toggle follow category (Add)
        mockMvc.perform(post("/api/v1/users/follow/" + testCategory.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followed").value(true));

        // Toggle follow category (Remove)
        mockMvc.perform(post("/api/v1/users/follow/" + testCategory.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followed").value(false));
    }

    @Test
    void commentsFlow_Success() throws Exception {
        // Create comment
        CommentRequest request = CommentRequest.builder()
                .eventId(testEvent.getId())
                .content("Fascinating event!")
                .build();

        String commentJson = mockMvc.perform(post("/api/v1/comments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Fascinating event!"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andReturn().getResponse().getContentAsString();

        String commentId = objectMapper.readTree(commentJson).get("id").asText();

        // Get comments for event
        mockMvc.perform(get("/api/v1/comments/event/" + testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Fascinating event!"));

        // Edit comment
        mockMvc.perform(put("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Very fascinating event!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Very fascinating event!"))
                .andExpect(jsonPath("$.isEdited").value(true));

        // Delete comment
        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void categoriesController_Success() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("History"));

        mockMvc.perform(get("/api/v1/categories/" + testCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("History"));
    }

    @Test
    void adminOperations_Success() throws Exception {
        // Create event
        EventRequestDto request = EventRequestDto.builder()
                .title("New Historical Event")
                .summary("Summary text")
                .detailedContent("Detailed details")
                .categories(Set.of("Science"))
                .build();

        String eventJson = mockMvc.perform(post("/api/v1/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Historical Event"))
                .andReturn().getResponse().getContentAsString();

        String eventId = objectMapper.readTree(eventJson).get("id").asText();

        // Update event
        request.setTitle("Updated Title");
        mockMvc.perform(put("/api/v1/admin/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));

        // Update user status
        mockMvc.perform(put("/api/v1/admin/users/" + testUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "locked"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User status updated to locked"));

        // Delete event
        mockMvc.perform(delete("/api/v1/admin/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void interactionsFlow_Success() throws Exception {
        InteractionRequest request = InteractionRequest.builder()
                .eventId(testEvent.getId())
                .interactionType("VIEW")
                .build();

        // Anonymous VIEW interaction
        mockMvc.perform(post("/api/v1/feed/interact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Authenticated LIKE interaction
        request.setInteractionType("LIKE");
        mockMvc.perform(post("/api/v1/feed/interact")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
