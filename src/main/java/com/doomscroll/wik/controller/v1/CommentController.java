package com.doomscroll.wik.controller.v1;

import com.doomscroll.wik.dto.CommentDto;
import com.doomscroll.wik.dto.request.comment.CommentRequest;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.service.social.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Endpoints for creating and managing comments and replies")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Create a new comment or threaded reply")
    public ResponseEntity<CommentDto> createComment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CommentRequest request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(commentService.createComment(user.getId(), request));
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get top-level comments for a historical event")
    public ResponseEntity<Page<CommentDto>> getCommentsForEvent(
            @PathVariable UUID eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commentService.getCommentsForEvent(eventId, PageRequest.of(page, size)));
    }

    @GetMapping("/{commentId}/replies")
    @Operation(summary = "Get replies for a specific parent comment")
    public ResponseEntity<List<CommentDto>> getRepliesForComment(@PathVariable UUID commentId) {
        return ResponseEntity.ok(commentService.getRepliesForComment(commentId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit an existing comment's content")
    public ResponseEntity<CommentDto> editComment(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(commentService.editComment(user.getId(), id, content));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (soft-delete) a comment")
    public ResponseEntity<Map<String, String>> deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        commentService.deleteComment(user.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
    }
}
