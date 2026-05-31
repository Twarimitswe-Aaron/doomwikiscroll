package com.doomscroll.wik.repository;

import com.doomscroll.wik.entity.ReadingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, UUID> {
    Optional<ReadingHistory> findByUserIdAndEventId(UUID userId, UUID eventId);
}
