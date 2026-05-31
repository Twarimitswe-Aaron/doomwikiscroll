package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findByEventIdAndParentCommentIsNullAndIsDeletedFalse(UUID eventId, Pageable pageable);
    List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID parentCommentId);
}
