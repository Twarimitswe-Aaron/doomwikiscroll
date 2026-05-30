package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.entity.HistoricalEvent;
import com.doomscroll.wik.repository.HistoricalEventRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HistoricalEventRepository eventRepository;

    private HistoricalEvent testEvent;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();

        testEvent = HistoricalEvent.builder()
                .title("Test Event")
                .summary("This is a test summary")
                .detailedContent("This is detailed content for the test event")
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
    void getEventById_Success() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + testEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEvent.getId().toString()))
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    void getEventById_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/events/" + java.util.UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()); // Handled by GlobalExceptionHandler
    }

    @Test
    void searchEvents_Success() throws Exception {
        mockMvc.perform(get("/api/v1/events/search")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Event"));
    }
}
