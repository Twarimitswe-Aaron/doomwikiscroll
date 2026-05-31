package com.doomscroll.wik.service.admin;

import com.doomscroll.wik.dto.EventDto;
import com.doomscroll.wik.dto.request.event.EventRequestDto;
import com.doomscroll.wik.entity.Category;
import com.doomscroll.wik.entity.HistoricalEvent;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.entity.enums.UserStatus;
import com.doomscroll.wik.mapper.EventMapper;
import com.doomscroll.wik.repository.CategoryRepository;
import com.doomscroll.wik.repository.HistoricalEventRepository;
import com.doomscroll.wik.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final HistoricalEventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    @Transactional
    public EventDto createEvent(EventRequestDto request) {
        HistoricalEvent event = HistoricalEvent.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .detailedContent(request.getDetailedContent())
                .eventDate(request.getEventDate())
                .eventYear(request.getEventYear())
                .era(request.getEra())
                .location(request.getLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .wikipediaUrl(request.getWikipediaUrl())
                .imageUrl(request.getImageUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .source("MANUAL")
                .viewCount(0L)
                .likeCount(0L)
                .commentCount(0L)
                .shareCount(0L)
                .isTrending(false)
                .isFeatured(false)
                .status("PUBLISHED")
                .build();

        if (request.getCategories() != null) {
            Set<Category> categories = new HashSet<>();
            for (String categoryName : request.getCategories()) {
                Category category = categoryRepository.findByName(categoryName)
                        .orElseGet(() -> categoryRepository.save(Category.builder()
                                .name(categoryName)
                                .isActive(true)
                                .build()));
                categories.add(category);
            }
            event.setCategories(categories);
        }

        event = eventRepository.save(event);
        return eventMapper.toDto(event);
    }

    @Transactional
    public EventDto updateEvent(UUID eventId, EventRequestDto request) {
        HistoricalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setTitle(request.getTitle());
        event.setSummary(request.getSummary());
        event.setDetailedContent(request.getDetailedContent());
        event.setEventDate(request.getEventDate());
        event.setEventYear(request.getEventYear());
        event.setEra(request.getEra());
        event.setLocation(request.getLocation());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
        event.setWikipediaUrl(request.getWikipediaUrl());
        event.setImageUrl(request.getImageUrl());
        event.setThumbnailUrl(request.getThumbnailUrl());

        if (request.getCategories() != null) {
            Set<Category> categories = new HashSet<>();
            for (String categoryName : request.getCategories()) {
                Category category = categoryRepository.findByName(categoryName)
                        .orElseGet(() -> categoryRepository.save(Category.builder()
                                .name(categoryName)
                                .isActive(true)
                                .build()));
                categories.add(category);
            }
            event.setCategories(categories);
        }

        event = eventRepository.save(event);
        return eventMapper.toDto(event);
    }

    @Transactional
    public void deleteEvent(UUID eventId) {
        HistoricalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        eventRepository.delete(event);
    }

    @Transactional
    public void updateUserStatus(UUID userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.valueOf(status.toUpperCase()));
        userRepository.save(user);
    }
}
