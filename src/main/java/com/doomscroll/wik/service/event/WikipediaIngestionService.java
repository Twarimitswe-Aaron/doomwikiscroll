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

    /**
     * Fetches random Wikipedia articles, strongly preferring those that have a thumbnail image.
     * Loops up to MAX_ROUNDS API calls (each returning up to 50 articles) until we have
     * collected `limit` image-bearing articles. Text-only articles are used only as a last-resort
     * safety fallback so the feed always returns something.
     */
    public List<EventDto> fetchRandomArticles(int limit) {
        log.info("fetchRandomArticles called with limit={}", limit);

        final int MAX_ROUNDS = 5;
        List<EventDto> withImage    = new ArrayList<>();
        List<EventDto> withoutImage = new ArrayList<>();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", userAgent);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (int round = 0; round < MAX_ROUNDS && withImage.size() < limit; round++) {
            try {
                java.net.URI uri = UriComponentsBuilder.fromHttpUrl(wikipediaApiUrl)
                        .queryParam("action", "query")
                        .queryParam("generator", "random")
                        .queryParam("grnnamespace", "0")
                        .queryParam("grnlimit", 50)          // max allowed by Wikipedia API
                        .queryParam("prop", "extracts|pageimages")
                        .queryParam("exintro", "1")
                        .queryParam("explaintext", "1")
                        .queryParam("exsentences", "3")
                        .queryParam("pithumbsize", "600")
                        .queryParam("format", "json")
                        .build()
                        .toUri();

                log.info("Round {} – fetching from Wikipedia: {}", round + 1, uri);

                ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(
                        uri, HttpMethod.GET, entity, JsonNode.class);

                JsonNode response = responseEntity.getBody();

                if (response != null && response.has("query") && response.get("query").has("pages")) {
                    JsonNode pages = response.get("query").get("pages");
                    log.info("Round {} – got {} pages", round + 1, pages.size());

                    pages.fields().forEachRemaining(entry -> {
                        JsonNode page = entry.getValue();
                        try {
                            EventDto event = mapToEventDto(page);
                            if (event != null) {
                                event.setId(UUID.randomUUID());
                                if (event.getThumbnailUrl() != null) {
                                    withImage.add(event);
                                } else {
                                    withoutImage.add(event);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to map page {}", entry.getKey(), e);
                        }
                    });

                    log.info("Round {} complete – {} with image, {} without (need {})",
                            round + 1, withImage.size(), withoutImage.size(), limit);
                } else {
                    log.warn("Round {} – Wikipedia response missing query.pages", round + 1);
                }

            } catch (Exception e) {
                log.error("Round {} – failed to fetch from Wikipedia", round + 1, e);
            }
        }

        // Build final list: image-bearing articles first, text-only as last resort
        List<EventDto> selected = new ArrayList<>(withImage);
        if (selected.size() < limit) {
            int remaining = limit - selected.size();
            selected.addAll(withoutImage.subList(0, Math.min(remaining, withoutImage.size())));
            log.warn("Only {} image articles found after {} rounds – padded with {} text-only",
                    withImage.size(), MAX_ROUNDS, selected.size() - withImage.size());
        }
        if (selected.size() > limit) {
            selected = selected.subList(0, limit);
        }

        // Cache selected events in Redis
        for (EventDto event : selected) {
            redisTemplate.opsForValue().set("temp_event:" + event.getId(), event, 24, TimeUnit.HOURS);
        }

        log.info("Returning {}/{} events (all with images: {})",
                selected.size(), limit, selected.size() <= withImage.size());
        return selected;
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
