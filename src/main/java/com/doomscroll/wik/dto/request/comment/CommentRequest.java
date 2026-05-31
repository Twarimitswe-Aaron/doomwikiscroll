package com.doomscroll.wik.dto.request.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotNull(message = "Event ID cannot be null")
    private UUID eventId;

    @NotBlank(message = "Comment content cannot be blank")
    @Size(max = 1000, message = "Comment content cannot exceed 1000 characters")
    private String content;

    private UUID parentCommentId;
}
