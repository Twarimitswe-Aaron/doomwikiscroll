package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.dto.request.InteractionRequest;
import com.doomscroll.wik.entity.HistoricalEvent;
import com.doomscroll.wik.repository.HistoricalEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class FeedControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HistoricalEventRepository eventRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.doomscroll.wik.service.event.WikipediaIngestionService wikipediaIngestionService;
    
    private HistoricalEvent testEvent;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        org.mockito.Mockito.when(wikipediaIngestionService.fetchRandomArticles(org.mockito.Mockito.anyInt()))
                .thenReturn(new java.util.ArrayList<>());
        org.mockito.Mockito.when(wikipediaIngestionService.ensureEventPersisted(org.mockito.Mockito.any(java.util.UUID.class)))
                .thenAnswer(invocation -> {
                    java.util.UUID id = invocation.getArgument(0);
                    return eventRepository.findById(id).orElse(null);
                });

        testEvent = HistoricalEvent.builder()
                .title("Feed Test Event")
                .summary("This is a test summary for feed")
                .detailedContent("This is detailed content for the feed test event")
                .source("WIKIPEDIA")
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .shareCount(0L)
                .isTrending(false)
                .isFeatured(false)
                .status("PUBLISHED")
                .build();
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    void getFeed_Success() throws Exception {
        mockMvc.perform(get("/api/v1/feed")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.events[*].title", org.hamcrest.Matchers.hasItem("Feed Test Event")))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").exists())
                .andExpect(jsonPath("$.totalElements").value(1000000));
    }

    @Test
    void recordInteraction_Success() throws Exception {
        InteractionRequest request = new InteractionRequest();
        request.setEventId(testEvent.getId());
        request.setInteractionType("LIKE");

        mockMvc.perform(post("/api/v1/feed/interact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Interaction recorded"));
    }
    
    @Test
    void recordInteraction_InvalidType() throws Exception {
        InteractionRequest request = new InteractionRequest();
        request.setEventId(testEvent.getId());
        request.setInteractionType("INVALID");

        mockMvc.perform(post("/api/v1/feed/interact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
