package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.dto.response.FeedResponseDto;
import com.doomscroll.wik.dto.request.InteractionRequest;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.service.social.FeedService;
import com.doomscroll.wik.service.social.SocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "Endpoints for the main content feed")
public class FeedController {

    private final FeedService feedService;
    private final SocialService socialService;

    @GetMapping
    @Operation(summary = "Get historical events feed")
    public ResponseEntity<FeedResponseDto> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(feedService.getFeed(page, size));
    }

    @PostMapping("/interact")
    @Operation(summary = "Record user interaction with an event")
    public ResponseEntity<Map<String, String>> recordInteraction(
            @Valid @RequestBody InteractionRequest request,
            @AuthenticationPrincipal User user) {
        socialService.recordInteraction(user != null ? user.getId() : null, request);
        return ResponseEntity.ok(Map.of("message", "Interaction recorded"));
    }
}
