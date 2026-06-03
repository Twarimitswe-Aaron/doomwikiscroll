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

    @Transactional(readOnly = true)
    public FeedResponseDto getFeed(int page, int size) {
        double seed = ThreadLocalRandom.current().nextDouble();
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<HistoricalEvent> eventPage = eventRepository.findPublishedEventsAfterRandomKey(seed, pageRequest);

        if (eventPage.isEmpty()) {
            eventPage = eventRepository.findPublishedEventsBeforeRandomKey(seed, pageRequest);
        }
        
        List<EventDto> events = eventPage.getContent().stream()
                .map(eventMapper::toDto)
                .collect(Collectors.toList());

        return FeedResponseDto.builder()
                .events(events)
                .pageNumber(eventPage.getNumber())
                .pageSize(eventPage.getSize())
                .hasNext(eventPage.hasNext())
                .totalElements(eventPage.getTotalElements())
                .build();
    }
}
