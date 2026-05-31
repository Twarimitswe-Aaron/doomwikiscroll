package com.doomscroll.wik.service.event;

import com.doomscroll.wik.dto.EventDto;
import com.doomscroll.wik.entity.HistoricalEvent;
import com.doomscroll.wik.mapper.EventMapper;
import com.doomscroll.wik.repository.HistoricalEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final HistoricalEventRepository eventRepository;
    private final EventMapper eventMapper;

    @Transactional(readOnly = true)
    public EventDto getEventById(UUID id) {
        HistoricalEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return eventMapper.toDto(event);
    }

    @Transactional(readOnly = true)
    public Page<EventDto> searchEvents(String query, Pageable pageable) {
        return eventRepository.searchEvents(query, pageable)
                .map(eventMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<EventDto> getEventsByCategory(UUID categoryId, Pageable pageable) {
        return eventRepository.findByCategoryId(categoryId, pageable)
                .map(eventMapper::toDto);
    }
}
