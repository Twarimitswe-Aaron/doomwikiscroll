package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.dto.EventDto;
import com.doomscroll.wik.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Historical Events", description = "Endpoints for managing historical events")
public class EventController {

    private final EventService eventService;

    @GetMapping("/{id}")
    @Operation(summary = "Get historical event by ID")
    public ResponseEntity<EventDto> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Search historical events")
    public ResponseEntity<Page<EventDto>> searchEvents(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.searchEvents(query, PageRequest.of(page, size)));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get historical events by category")
    public ResponseEntity<Page<EventDto>> getEventsByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(eventService.getEventsByCategory(categoryId, PageRequest.of(page, size)));
    }
}
