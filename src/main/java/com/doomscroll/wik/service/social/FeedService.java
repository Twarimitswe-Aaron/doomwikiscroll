package com.doomscroll.wik.service.social;

import com.doomscroll.wik.dto.response.FeedResponseDto;
import com.doomscroll.wik.dto.EventDto;
import com.doomscroll.wik.entity.HistoricalEvent;
import com.doomscroll.wik.mapper.EventMapper;
import com.doomscroll.wik.repository.HistoricalEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final HistoricalEventRepository eventRepository;
    private final EventMapper eventMapper;
    private final com.doomscroll.wik.service.event.WikipediaIngestionService wikipediaIngestionService;

    @Transactional(readOnly = true)
    public FeedResponseDto getFeed(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        
        // 1. Get some trending/popular events from DB
        Page<HistoricalEvent> dbEvents = eventRepository.findPublishedEventsAfterRandomKey(ThreadLocalRandom.current().nextDouble(), pageRequest);
        if (dbEvents.isEmpty()) {
            dbEvents = eventRepository.findPublishedEventsBeforeRandomKey(ThreadLocalRandom.current().nextDouble(), pageRequest);
        }
        
        List<EventDto> events = dbEvents.getContent().stream()
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
                
        // 2. Mix in fresh Wikipedia events
        int wikipediaNeeded = size - events.size();
        if (wikipediaNeeded < size / 2) {
            wikipediaNeeded = size / 2; // Always get at least some fresh Wikipedia content
        }
        
        List<EventDto> freshEvents = wikipediaIngestionService.fetchRandomArticles(wikipediaNeeded);
        events.addAll(freshEvents);

        // Shuffle the combined feed
        java.util.Collections.shuffle(events);

        return FeedResponseDto.builder()
                .events(events)
                .pageNumber(page)
                .pageSize(events.size())
                .hasNext(true) // Infinite scroll
                .totalElements(1000000L) // arbitrary large number for infinite scroll
                .build();
    }
}
