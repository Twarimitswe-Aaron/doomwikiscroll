package com.doomscroll.wik.service.event;

import com.doomscroll.wik.dto.EventDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import com.doomscroll.wik.entity.HistoricalEvent;
import com.doomscroll.wik.repository.HistoricalEventRepository;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class WikipediaIngestionService {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HistoricalEventRepository eventRepository;

    @Value("${wikipedia.api.base-url:https://en.wikipedia.org/w/api.php}")
    private String wikipediaApiUrl;

    @Value("${wikipedia.api.user-agent:WikHistoryFeed/1.0}")
    private String userAgent;

    public List<EventDto> fetchRandomArticles(int limit) {
        log.info("fetchRandomArticles called with limit={}", limit);
        List<EventDto> events = new ArrayList<>();
        try {
            java.net.URI uri = UriComponentsBuilder.fromHttpUrl(wikipediaApiUrl)
                    .queryParam("action", "query")
                    .queryParam("generator", "random")
                    .queryParam("grnnamespace", "0")
                    .queryParam("grnlimit", limit)
                    .queryParam("prop", "extracts|pageimages")
                    .queryParam("exintro", "1")
                    .queryParam("explaintext", "1")
                    .queryParam("exsentences", "3")
                    .queryParam("pithumbsize", "500")
                    .queryParam("format", "json")
                    .build()
                    .toUri();

            log.info("Fetching articles from Wikipedia API URL: {}", uri);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );
            
            log.info("Wikipedia API response status code: {}", responseEntity.getStatusCode());
            JsonNode response = responseEntity.getBody();
            log.debug("Wikipedia API response body: {}", response);

            if (response != null && response.has("query") && response.get("query").has("pages")) {
                JsonNode pages = response.get("query").get("pages");
                log.info("Found {} pages in Wikipedia response", pages.size());
                pages.fields().forEachRemaining(entry -> {
                    JsonNode page = entry.getValue();
                    try {
                        EventDto event = mapToEventDto(page);
                        if (event != null) {
                            event.setId(UUID.randomUUID());
                            redisTemplate.opsForValue().set("temp_event:" + event.getId(), event, 24, TimeUnit.HOURS);
                            events.add(event);
                            log.info("Successfully mapped and cached Wikipedia article: {}", event.getTitle());
                        } else {
                            log.warn("Skipped page ID: {} (null mapped EventDto, possibly missing extract)", entry.getKey());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to map Wikipedia page ID: {}", entry.getKey(), e);
                    }
                });
            } else {
                log.warn("Wikipedia response did not contain query.pages: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to fetch articles from Wikipedia", e);
        }
        log.info("Returning {} events from fetchRandomArticles", events.size());
        return events;
    }

    private EventDto mapToEventDto(JsonNode page) {
        if (!page.has("extract") || page.get("extract").asText().isBlank()) {
            return null; // Skip articles without summaries
        }

        Long pageId = page.get("pageid").asLong();
        String title = page.get("title").asText();
        String summary = page.get("extract").asText();
        String wikipediaUrl = "https://en.wikipedia.org/?curid=" + pageId;

        String thumbnailUrl = null;
        if (page.has("thumbnail")) {
            thumbnailUrl = page.get("thumbnail").get("source").asText();
        }

        return EventDto.builder()
                .wikipediaPageId(pageId)
                .title(title)
                .summary(summary)
                .wikipediaUrl(wikipediaUrl)
                .imageUrl(thumbnailUrl)
                .thumbnailUrl(thumbnailUrl)
                .source("WIKIPEDIA")
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .shareCount(0L)
                .build();
    }

    @org.springframework.transaction.annotation.Transactional
    public HistoricalEvent ensureEventPersisted(UUID eventId) {
        HistoricalEvent event = eventRepository.findById(eventId).orElse(null);
        if (event != null) {
            return event;
        }

        EventDto cachedEvent = (EventDto) redisTemplate.opsForValue().get("temp_event:" + eventId);
        if (cachedEvent != null) {
            HistoricalEvent newEvent = HistoricalEvent.builder()
                    .title(cachedEvent.getTitle())
                    .summary(cachedEvent.getSummary())
                    .wikipediaUrl(cachedEvent.getWikipediaUrl())
                    .wikipediaPageId(cachedEvent.getWikipediaPageId())
                    .imageUrl(cachedEvent.getImageUrl())
                    .thumbnailUrl(cachedEvent.getThumbnailUrl())
                    .source(cachedEvent.getSource())
                    .build();
                    
            newEvent.setId(cachedEvent.getId());
            newEvent = eventRepository.save(newEvent);
            redisTemplate.delete("temp_event:" + eventId);
            return newEvent;
        }
        
        throw new RuntimeException("Event not found");
    }
}
