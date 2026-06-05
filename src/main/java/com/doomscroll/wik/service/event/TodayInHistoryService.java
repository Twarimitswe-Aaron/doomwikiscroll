package com.doomscroll.wik.service.event;

import com.doomscroll.wik.dto.TodayStoryDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fetches "Today in History" stories using two Wikipedia sources:
 *
 *  1. /feed/featured/{year}/{month}/{day} → "In the News" (news items from today)
 *  2. /feed/onthisday/selected/{month}/{day} → Wikipedia's hand-picked significant events
 *
 * Both are filtered to image-bearing articles and cached in Redis for 6 hours.
 * Max 8 stories total, prioritising today's news first.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TodayInHistoryService {

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${wikipedia.api.user-agent:WikHistoryFeed/1.0}")
    private String userAgent;

    private static final int MAX_STORIES       = 8;
    private static final String CACHE_KEY_PREFIX = "today_stories_v2:";

    @SuppressWarnings("unchecked")
    public List<TodayStoryDto> getTodayStories() {
        LocalDate today = LocalDate.now();
        String cacheKey = CACHE_KEY_PREFIX + today;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            log.debug("Returning cached today stories for {}", today);
            return (List<TodayStoryDto>) cached;
        }

        List<TodayStoryDto> stories = buildStories(today);

        if (!stories.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, stories, 6, TimeUnit.HOURS);
            log.info("Cached {} today stories", stories.size());
        }
        return stories;
    }

    // ──────────────────────────────────────────────────────────────
    //  Build story list from two Wikipedia sources
    // ──────────────────────────────────────────────────────────────

    private List<TodayStoryDto> buildStories(LocalDate today) {
        List<TodayStoryDto> result = new ArrayList<>();

        // 1️⃣  Today's news ("In the News") from Wikipedia featured content
        result.addAll(fetchTodaysNews(today));

        // 2️⃣  Fill remaining slots with Wikipedia's curated "selected" anniversaries
        if (result.size() < MAX_STORIES) {
            List<TodayStoryDto> selected = fetchSelectedAnniversaries(today.getMonthValue(), today.getDayOfMonth());
            for (TodayStoryDto s : selected) {
                if (result.size() >= MAX_STORIES) break;
                // Avoid duplicates
                boolean dup = result.stream().anyMatch(r -> r.getId().equals(s.getId()));
                if (!dup) result.add(s);
            }
        }

        log.info("Returning {} today stories total", result.size());
        return result;
    }

    // ──────────────────────────────────────────────────────────────
    //  Source 1: Wikipedia Featured Content — "In the News"
    // ──────────────────────────────────────────────────────────────

    private List<TodayStoryDto> fetchTodaysNews(LocalDate today) {
        String url = String.format(
                "https://en.wikipedia.org/api/rest_v1/feed/featured/%d/%02d/%02d",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        log.info("Fetching today's news from: {}", url);

        List<TodayStoryDto> withImage    = new ArrayList<>();
        List<TodayStoryDto> withoutImage = new ArrayList<>();

        try {
            JsonNode body = get(url);
            if (body == null) return List.of();

            // "news" field: array of news items, each with links[]
            if (body.has("news")) {
                for (JsonNode newsItem : body.get("news")) {
                    try {
                        TodayStoryDto story = mapNewsItem(newsItem);
                        if (story != null) {
                            if (story.getThumbnailUrl() != null) withImage.add(story);
                            else withoutImage.add(story);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to map news item", e);
                    }
                }
            }

            // "mostread" articles as secondary source if news is empty
            if (withImage.isEmpty() && body.has("mostread")) {
                JsonNode articles = body.get("mostread").get("articles");
                if (articles != null) {
                    for (JsonNode article : articles) {
                        try {
                            TodayStoryDto story = mapArticleNode(article, null);
                            if (story != null) {
                                if (story.getThumbnailUrl() != null) withImage.add(story);
                                else withoutImage.add(story);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to map mostread article", e);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to fetch today's news from Wikipedia", e);
        }

        List<TodayStoryDto> result = new ArrayList<>(withImage);
        result.addAll(withoutImage);
        return result.size() > MAX_STORIES ? result.subList(0, MAX_STORIES) : result;
    }

    private TodayStoryDto mapNewsItem(JsonNode newsItem) {
        String rawStory = newsItem.has("story") ? newsItem.get("story").asText() : null;
        String extract  = rawStory != null ? stripHtml(rawStory) : null;

        // Pick the first link that has a thumbnail
        if (newsItem.has("links") && newsItem.get("links").isArray()) {
            for (JsonNode link : newsItem.get("links")) {
                TodayStoryDto story = mapArticleNode(link, extract);
                if (story != null) return story;
            }
        }
        return null;
    }

    // ──────────────────────────────────────────────────────────────
    //  Source 2: Wikipedia "selected" anniversaries for this day
    // ──────────────────────────────────────────────────────────────

    private List<TodayStoryDto> fetchSelectedAnniversaries(int month, int day) {
        // Wikipedia curates only ~5-10 truly significant events per date
        String url = String.format(
                "https://en.wikipedia.org/api/rest_v1/feed/onthisday/selected/%d/%d", month, day);
        log.info("Fetching selected anniversaries from: {}", url);

        List<TodayStoryDto> withImage    = new ArrayList<>();
        List<TodayStoryDto> withoutImage = new ArrayList<>();

        try {
            JsonNode body = get(url);
            if (body == null || !body.has("selected")) return List.of();

            for (JsonNode event : body.get("selected")) {
                try {
                    TodayStoryDto story = mapEventNode(event);
                    if (story != null) {
                        if (story.getThumbnailUrl() != null) withImage.add(story);
                        else withoutImage.add(story);
                    }
                } catch (Exception e) {
                    log.warn("Failed to map selected anniversary", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch selected anniversaries from Wikipedia", e);
        }

        List<TodayStoryDto> result = new ArrayList<>(withImage);
        result.addAll(withoutImage);
        return result;
    }

    // ──────────────────────────────────────────────────────────────
    //  Mappers
    // ──────────────────────────────────────────────────────────────

    /** Map a Wikipedia "event" node (has year + pages[]) */
    private TodayStoryDto mapEventNode(JsonNode event) {
        if (!event.has("text") || event.get("text").asText().isBlank()) return null;

        Integer year    = event.has("year") ? event.get("year").asInt() : null;
        String  extract = event.get("text").asText();

        if (event.has("pages") && event.get("pages").isArray() && event.get("pages").size() > 0) {
            JsonNode page = event.get("pages").get(0);
            return mapArticleNode(page, extract, year);
        }
        return null;
    }

    /** Map a Wikipedia article node (thumbnail, title, url) */
    private TodayStoryDto mapArticleNode(JsonNode page, String fallbackExtract) {
        return mapArticleNode(page, fallbackExtract, null);
    }

    private TodayStoryDto mapArticleNode(JsonNode page, String fallbackExtract, Integer year) {
        if (page == null) return null;

        String title = page.has("normalizedtitle") ? page.get("normalizedtitle").asText()
                : page.has("title")                ? page.get("title").asText()
                : null;
        if (title == null || title.isBlank()) return null;

        String pageId = page.has("pageid") ? page.get("pageid").asText() : null;

        String wikiUrl = null;
        if (page.has("content_urls")) {
            JsonNode urls = page.get("content_urls");
            if (urls.has("desktop")) wikiUrl = urls.get("desktop").get("page").asText();
        }
        if (wikiUrl == null) {
            wikiUrl = "https://en.wikipedia.org/wiki/" + title.replace(" ", "_");
        }

        String thumbnailUrl = null;
        String imageUrl     = null;
        if (page.has("thumbnail")) {
            thumbnailUrl = page.get("thumbnail").get("source").asText();
        }
        if (page.has("originalimage")) {
            imageUrl = page.get("originalimage").get("source").asText();
        } else {
            imageUrl = thumbnailUrl;
        }

        String extract = fallbackExtract != null ? stripHtml(fallbackExtract) : null;
        if ((extract == null || extract.isBlank()) && page.has("extract")) {
            extract = stripHtml(page.get("extract").asText());
        }
        if (extract == null) extract = title;

        return TodayStoryDto.builder()
                .id(pageId != null ? pageId : String.valueOf(title.hashCode()))
                .title(title)
                .extract(extract)
                .year(year)
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .wikipediaUrl(wikiUrl)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    //  HTML stripping
    // ──────────────────────────────────────────────────────────────

    /**
     * Removes HTML/XML tags, XML comments, and decodes the most common
     * HTML entities so that the returned string is clean plain text.
     */
    private static String stripHtml(String html) {
        if (html == null || html.isBlank()) return html;
        return html
                // Remove XML/HTML comments <!-- ... -->
                .replaceAll("<!--.*?-->", "")
                // Remove all HTML/XML tags
                .replaceAll("<[^>]+>", "")
                // Decode common HTML entities
                .replace("&amp;",  "&")
                .replace("&lt;",   "<")
                .replace("&gt;",   ">")
                .replace("&quot;", "\"")
                .replace("&#39;",  "'")
                .replace("&nbsp;", " ")
                .replace("&ndash;","–")
                .replace("&mdash;","—")
                .replace("&rsquo;","'")
                .replace("&lsquo;","'")
                .replace("&rdquo;","\"")
                .replace("&ldquo;","\"")
                // Collapse multiple spaces / newlines
                .replaceAll("[ \t]+", " ")
                .trim();
    }

    // ──────────────────────────────────────────────────────────────
    //  HTTP helper
    // ──────────────────────────────────────────────────────────────

    private JsonNode get(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", userAgent);
        headers.set("Accept", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> resp = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        return resp.getBody();
    }
}
