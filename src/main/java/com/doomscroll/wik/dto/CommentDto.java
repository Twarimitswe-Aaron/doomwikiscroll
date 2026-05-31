package com.doomscroll.wik.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private UUID id;
    private String content;
    private UUID userId;
    private String username;
    private UUID eventId;
    private UUID parentCommentId;
    private Integer likeCount;
    private Boolean isEdited;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
