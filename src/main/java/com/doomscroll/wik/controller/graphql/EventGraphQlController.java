package com.doomscroll.wik.controller.graphql;

import com.doomscroll.wik.dto.EventDto;
import com.doomscroll.wik.dto.request.InteractionRequest;
import com.doomscroll.wik.dto.response.FeedResponseDto;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.service.event.EventService;
import com.doomscroll.wik.service.social.FeedService;
import com.doomscroll.wik.service.social.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class EventGraphQlController {

    private final EventService eventService;
    private final FeedService feedService;
    private final SocialService socialService;

    @QueryMapping
    public EventDto event(@Argument UUID id) {
        return eventService.getEventById(id);
    }

    @QueryMapping
    public List<EventDto> events(@Argument String query, @Argument int page, @Argument int size) {
        return eventService.searchEvents(query, PageRequest.of(page, size)).getContent();
    }

    @QueryMapping
    public List<EventDto> eventsByCategory(@Argument UUID categoryId, @Argument int page, @Argument int size) {
        return eventService.getEventsByCategory(categoryId, PageRequest.of(page, size)).getContent();
    }

    @QueryMapping
    public FeedResponseDto feed(@Argument int page, @Argument int size) {
        return feedService.getFeed(page, size);
    }

    @MutationMapping
    public Boolean recordInteraction(@Argument UUID eventId, @Argument String type) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = null;
        if (auth != null && auth.getPrincipal() instanceof User) {
            userId = ((User) auth.getPrincipal()).getId();
        }

        InteractionRequest request = InteractionRequest.builder()
                .eventId(eventId)
                .interactionType(type)
                .build();

        socialService.recordInteraction(userId, request);
        return true;
    }
}
