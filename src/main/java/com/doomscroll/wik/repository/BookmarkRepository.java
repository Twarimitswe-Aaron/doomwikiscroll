package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    Optional<Bookmark> findByUserIdAndEventId(UUID userId, UUID eventId);
    Page<Bookmark> findByUserId(UUID userId, Pageable pageable);
    boolean existsByUserIdAndEventId(UUID userId, UUID eventId);
}
