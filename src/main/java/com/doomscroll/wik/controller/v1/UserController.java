package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.dto.EventDto;
import com.doomscroll.wik.dto.UserProfileDto;
import com.doomscroll.wik.dto.request.user.UpdateProfileRequest;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.service.social.SocialService;
import com.doomscroll.wik.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for user profiles, bookmarks, and topic follows")
public class UserController {

    private final UserService userService;
    private final SocialService socialService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserProfileDto> getCurrentUserProfile(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.getCurrentUserProfile(user.getId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current authenticated user profile")
    public ResponseEntity<UserProfileDto> updateUserProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.updateUserProfile(user.getId(), request));
    }

    @GetMapping("/bookmarks")
    @Operation(summary = "Get bookmarks for current user")
    public ResponseEntity<Page<EventDto>> getBookmarks(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.getBookmarks(user.getId(), PageRequest.of(page, size)));
    }

    @PostMapping("/bookmarks/{eventId}")
    @Operation(summary = "Toggle bookmarking a historical event")
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @AuthenticationPrincipal User user,
            @PathVariable UUID eventId) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        boolean bookmarked = socialService.toggleBookmark(user.getId(), eventId);
        return ResponseEntity.ok(Map.of(
                "bookmarked", bookmarked,
                "message", bookmarked ? "Event bookmarked" : "Bookmark removed"
        ));
    }

    @PostMapping("/follow/{categoryId}")
    @Operation(summary = "Toggle following a topic category")
    public ResponseEntity<Map<String, Object>> toggleFollow(
            @AuthenticationPrincipal User user,
            @PathVariable UUID categoryId) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        boolean followed = socialService.toggleFollowCategory(user.getId(), categoryId);
        return ResponseEntity.ok(Map.of(
                "followed", followed,
                "message", followed ? "Category followed" : "Category unfollowed"
        ));
    }
}
