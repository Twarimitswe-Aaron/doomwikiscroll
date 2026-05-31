package com.doomscroll.wik.controller.graphql;

import com.doomscroll.wik.dto.CategoryDto;
import com.doomscroll.wik.dto.CommentDto;
import com.doomscroll.wik.dto.UserProfileDto;
import com.doomscroll.wik.dto.request.comment.CommentRequest;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.service.event.CategoryService;
import com.doomscroll.wik.service.social.CommentService;
import com.doomscroll.wik.service.social.SocialService;
import com.doomscroll.wik.service.user.UserService;
import lombok.RequiredArgsConstructor;
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
public class UserGraphQlController {

    private final UserService userService;
    private final CategoryService categoryService;
    private final CommentService commentService;
    private final SocialService socialService;

    @QueryMapping
    public UserProfileDto me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new RuntimeException("Unauthorized");
        }
        User user = (User) auth.getPrincipal();
        return userService.getCurrentUserProfile(user.getId());
    }

    @QueryMapping
    public List<CategoryDto> categories() {
        return categoryService.getActiveCategories();
    }

    @MutationMapping
    public CommentDto addComment(
            @Argument UUID eventId,
            @Argument String content,
            @Argument UUID parentCommentId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new RuntimeException("Unauthorized");
        }
        User user = (User) auth.getPrincipal();

        CommentRequest request = CommentRequest.builder()
                .eventId(eventId)
                .content(content)
                .parentCommentId(parentCommentId)
                .build();

        return commentService.createComment(user.getId(), request);
    }

    @MutationMapping
    public Boolean toggleBookmark(@Argument UUID eventId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new RuntimeException("Unauthorized");
        }
        User user = (User) auth.getPrincipal();
        return socialService.toggleBookmark(user.getId(), eventId);
    }

    @MutationMapping
    public Boolean toggleFollowCategory(@Argument UUID categoryId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            throw new RuntimeException("Unauthorized");
        }
        User user = (User) auth.getPrincipal();
        return socialService.toggleFollowCategory(user.getId(), categoryId);
    }
}
