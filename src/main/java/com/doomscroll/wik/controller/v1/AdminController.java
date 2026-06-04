package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.dto.EventDto;
import com.doomscroll.wik.dto.request.event.EventRequestDto;
import com.doomscroll.wik.service.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "Administrative operations for content moderation and user management")
public class AdminController {

    private final AdminService adminService;

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Change a user's account status (e.g. active, locked)")
    public ResponseEntity<Map<String, String>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        adminService.updateUserStatus(userId, status);
        return ResponseEntity.ok(Map.of("message", "User status updated to " + status));
    }

    @PostMapping("/events")
    @Operation(summary = "Create a historical event manually")
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventRequestDto request) {
        return ResponseEntity.ok(adminService.createEvent(request));
    }

    @PutMapping("/events/{eventId}")
    @Operation(summary = "Update an existing historical event")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventRequestDto request) {
        return ResponseEntity.ok(adminService.updateEvent(eventId, request));
    }

    @DeleteMapping("/events/{eventId}")
    @Operation(summary = "Delete a historical event")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        adminService.deleteEvent(eventId);
        return ResponseEntity.ok().build();
    }
}
