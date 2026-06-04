package com.doomscroll.wik.service.social;

import com.doomscroll.wik.dto.CommentDto;
import com.doomscroll.wik.dto.request.comment.CommentRequest;
import com.doomscroll.wik.entity.Comment;
import com.doomscroll.wik.entity.HistoricalEvent;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.repository.CommentRepository;
import com.doomscroll.wik.repository.HistoricalEventRepository;
import com.doomscroll.wik.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final HistoricalEventRepository eventRepository;
    private final com.doomscroll.wik.service.event.WikipediaIngestionService wikipediaIngestionService;

    @Transactional
    public CommentDto createComment(UUID userId, CommentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        HistoricalEvent event = wikipediaIngestionService.ensureEventPersisted(request.getEventId());

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
        }

        Comment comment = Comment.builder()
                .content(request.getContent())
                .user(user)
                .event(event)
                .parentComment(parentComment)
                .likeCount(0)
                .isEdited(false)
                .isDeleted(false)
                .build();

        comment = commentRepository.save(comment);

        // Update event comment count
        event.setCommentCount(event.getCommentCount() + 1);
        eventRepository.save(event);

        return toDto(comment);
    }

    @Transactional(readOnly = true)
    public Page<CommentDto> getCommentsForEvent(UUID eventId, Pageable pageable) {
        return commentRepository.findByEventIdAndParentCommentIsNullAndIsDeletedFalse(eventId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getRepliesForComment(UUID commentId) {
        return commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(commentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDto editComment(UUID userId, UUID commentId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to edit this comment");
        }

        comment.setContent(content);
        comment.setIsEdited(true);
        comment = commentRepository.save(comment);

        return toDto(comment);
    }

    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);

        // Decrement comment count on event
        HistoricalEvent event = comment.getEvent();
        if (event.getCommentCount() > 0) {
            event.setCommentCount(event.getCommentCount() - 1);
            eventRepository.save(event);
        }
    }

    public CommentDto toDto(Comment comment) {
        if (comment == null) return null;
        return CommentDto.builder()
                .id(comment.getId())
                .content(comment.getIsDeleted() ? "[Comment deleted]" : comment.getContent())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getDisplayUsername())
                .eventId(comment.getEvent().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .likeCount(comment.getLikeCount())
                .isEdited(comment.getIsEdited())
                .isDeleted(comment.getIsDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
